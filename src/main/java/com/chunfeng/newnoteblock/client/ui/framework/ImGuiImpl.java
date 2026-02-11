package com.chunfeng.newnoteblock.client.ui.framework;

import com.chunfeng.newnoteblock.client.ui.screen.NewNoteBlockScreen;
import com.chunfeng.newnoteblock.client.ui.screen.WorldEditScreen;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.apache.commons.compress.utils.IOUtils;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

public class ImGuiImpl {
    public static ImGuiImpl INSTANCE;

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private boolean isInitialized = false;

    // --- 字体存储 ---
    public ImFont fontRegular; // 30px
    public ImFont fontTitle; // 80px
    public ImFont fontSmall; // 20px

    // [关键修复] 持有 GlyphRanges 的强引用，防止被 JVM 垃圾回收导致 C++ 端访问野指针
    private static short[] sharedGlyphRanges;

    public static void create(long windowId) {
        if (INSTANCE == null) {
            INSTANCE = new ImGuiImpl();
            INSTANCE.init(windowId);
        }
    }

    private void init(long windowId) {
        if (isInitialized)
            return;

        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();

        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);

        // [关键修复] 扩大字体纹理图集大小为 8192x8192 -> 降级为 4096 以防止显存溢出
        // 加载两套全量中文 (30px + 20px) 极其消耗显存，默认的 4096 可能不够用，但在添加大量Block纹理后 8192 可能导致 crash
        io.getFonts().setTexDesiredWidth(4096);

        // [关键修复] 初始化全局字形范围，确保引用不丢失
        if (sharedGlyphRanges == null) {
            sharedGlyphRanges = GlyphRanges.getAllRanges();
        }

        // --- 字体加载 ---
        try {
            // 确保该路径下文件存在 (WenQuanWeiMiHei.ttf)
            String fontPath = "/assets/newnoteblock/fonts/WenQuanWeiMiHei.ttf";
            System.out.println("NewNoteBlock: 正在加载字体: " + fontPath);

            // 1. Regular (30px): 界面主体，需要中文 -> 加载全量范围
            fontRegular = loadFont(fontPath, 30, true);

            // 2. Title (80px): 仅用于顶部英文标题 -> ！！！只加载英文！！！
            fontTitle = loadFont(fontPath, 80, false);

            // 3. Small (20px): 钢琴键和微调，需要中文 -> 加载全量范围
            fontSmall = loadFont(fontPath, 20, true);

            // [关键修复] 显式触发一次构建，确保图集生成成功，如有问题会立即抛出
            io.getFonts().build();

            System.out.println("NewNoteBlock: 字体加载完成 (Title字体已优化, Texture Size: 8192)");
        } catch (Exception e) {
            System.err.println("NewNoteBlock: 字体加载失败，回退默认。Error: " + e.getMessage());
            e.printStackTrace();

            // 如果构建失败（例如显卡不支持 8k 纹理），回退到默认字体
            io.getFonts().clear();
            ImFontConfig config = new ImFontConfig();
            fontRegular = io.getFonts().addFontDefault(config);
            fontTitle = fontRegular;
            fontSmall = fontRegular;
            io.setFontGlobalScale(2.0f);
            config.destroy();
        }

        imGuiGlfw.init(windowId, true);
        imGuiGl3.init("#version 150");

        isInitialized = true;
    }

    /**
     * 加载字体
     * 
     * @param path        路径
     * @param pixelSize   大小
     * @param loadChinese 是否加载中文 (Title字体选 false 以节省显存)
     */
    private static ImFont loadFont(final String path, final int pixelSize, boolean loadChinese) {
        final ImFontConfig config = new ImFontConfig();

        // 核心优化逻辑
        if (loadChinese) {
            // [关键修复] 使用静态成员变量 sharedGlyphRanges，而不是临时调用 GlyphRanges.getAllRanges()
            config.setGlyphRanges(sharedGlyphRanges);
        } else {
            // 不需要中文时，只加载默认 ASCII
            config.setGlyphRanges(ImGui.getIO().getFonts().getGlyphRangesDefault());
        }

        config.setPixelSnapH(true);
        // [建议] 如果字体仍然崩溃，可以尝试开启此选项让 ImGui 管理数据内存，但在 Java 端通常不需要
        // config.setFontDataOwnedByAtlas(false);

        try (final InputStream in = Objects.requireNonNull(ImGuiImpl.class.getResourceAsStream(path))) {
            final byte[] fontData = IOUtils.toByteArray(in);
            return ImGui.getIO().getFonts().addFontFromMemoryTTF(fontData, pixelSize, config);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to load font from path: " + path, e);
        } finally {
            config.destroy();
        }
    }

    public void render() {
        if (!isInitialized)
            return;

        // [安全检查] 确保字体图集已构建
        if (!ImGui.getIO().getFonts().isBuilt()) {
            System.err.println("NewNoteBlock Warning: Font Atlas not built!");
            return;
        }

        imGuiGlfw.newFrame();
        ImGui.newFrame();
        NewNoteBlockScreen.render();
        // 渲染批量编辑器
        WorldEditScreen.render();
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long ptr = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(ptr);
        }
    }

    public void dispose() {
        if (!isInitialized)
            return;
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
        isInitialized = false;
    }
}