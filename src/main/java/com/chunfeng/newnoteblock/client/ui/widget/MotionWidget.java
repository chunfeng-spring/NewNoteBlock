package com.chunfeng.newnoteblock.client.ui.widget;

import com.chunfeng.newnoteblock.client.ui.data.NoteData;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import net.minecraft.util.math.Vec3d;
import com.chunfeng.newnoteblock.util.MotionCalculator;

import java.util.List;

public class MotionWidget {

    private static String errorMessage = "";

    public static void render(NoteData data) {
        if (ImGui.beginTable("MotionLayout", 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.Resizable)) {
            ImGui.tableSetupColumn("Controls", ImGuiTableColumnFlags.WidthStretch, 0.4f);
            ImGui.tableSetupColumn("Preview", ImGuiTableColumnFlags.WidthStretch, 0.6f);

            ImGui.tableNextColumn();
            renderControls(data);

            ImGui.tableNextColumn();
            renderPreview(data);

            ImGui.endTable();
        }
    }

    private static void renderControls(NoteData data) {
        ImGui.text("声源移动轨迹表达式 (变量: t)");
        ImGui.separator();

        ImGui.pushItemWidth(-1);

        ImGui.text("X(t) = ");
        ImGui.inputText("##ExpX", data.motionExpX);

        ImGui.text("Y(t) = ");
        ImGui.inputText("##ExpY", data.motionExpY);

        ImGui.text("Z(t) = ");
        ImGui.inputText("##ExpZ", data.motionExpZ);

        ImGui.dummy(0, 5);

        // [新增] 运动模式选择
        ImGui.alignTextToFramePadding();
        ImGui.text("模式选择：");
        ImGui.sameLine();
        ImGui.pushItemWidth(-1);
        if (ImGui.beginCombo("##MotionMode", data.motionMode.get() ? "相对坐标模式" : "绝对坐标模式")) {
            if (ImGui.selectable("相对坐标模式", data.motionMode.get())) {
                data.motionMode.set(true);
            }
            if (ImGui.selectable("绝对坐标模式", !data.motionMode.get())) {
                data.motionMode.set(false);
            }
            ImGui.endCombo();
        }
        ImGui.popItemWidth();
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(data.motionMode.get() ? "当前: 相对坐标 (相对于音符盒位置)" : "当前: 绝对坐标 (世界坐标)");
        }

        ImGui.text("定义域(Tick):");

        ImGui.popItemWidth();

        ImGui.pushItemWidth(140);

        ImGui.alignTextToFramePadding();
        ImGui.text("起始");
        ImGui.sameLine();
        if (ImGui.inputInt("##Start", data.motionStartTick)) {
            if (data.motionStartTick.get() < 0)
                data.motionStartTick.set(0);
        }

        ImGui.sameLine();

        ImGui.alignTextToFramePadding();
        ImGui.text("终止");
        ImGui.sameLine();
        if (ImGui.inputInt("##End", data.motionEndTick)) {
            if (data.motionEndTick.get() < 0)
                data.motionEndTick.set(0);
        }

        ImGui.popItemWidth();

        ImGui.dummy(0, 10);

        if (ImGui.button("生成预览轨迹", -1, 30)) {
            generateTrajectory(data);
        }

        if (!errorMessage.isEmpty()) {
            ImGui.textColored(1.0f, 0.3f, 0.3f, 1.0f, "错误: " + errorMessage);
        }
    }

    public static void generateTrajectory(NoteData data) {
        errorMessage = "";
        data.motionPreviewPath.clear();

        int startTick = data.motionStartTick.get();
        int endTick = data.motionEndTick.get();

        if (startTick > endTick) {
            errorMessage = "起始 Tick 不能大于终止 Tick";
            return;
        }

        try {
            List<Vec3d> path = MotionCalculator.calculate(
                    data.motionExpX.get(),
                    data.motionExpY.get(),
                    data.motionExpZ.get(),
                    startTick,
                    endTick);
            data.motionPreviewPath.addAll(path);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
        }
    }

    private static void renderPreview(NoteData data) {
        ImGui.text("声源 3D 轨迹预览");

        ImDrawList drawList = ImGui.getWindowDrawList();
        float startX = ImGui.getCursorScreenPosX();
        float startY = ImGui.getCursorScreenPosY();
        float w = ImGui.getContentRegionAvailX();
        float h = ImGui.getContentRegionAvailY();

        drawList.addRectFilled(startX, startY, startX + w, startY + h, 0xFF111111);
        drawList.addRect(startX, startY, startX + w, startY + h, 0xFF555555);

        float centerX = startX + w / 2.0f;
        float centerY = startY + h / 2.0f;

        float scale = 20.0f;

        float originX = projectX(0, 0, 0, scale, centerX);
        float originY = projectY(0, 0, 0, scale, centerY);

        // X轴
        float xAxisX = projectX(5, 0, 0, scale, centerX);
        float xAxisY = projectY(5, 0, 0, scale, centerY);
        drawList.addLine(originX, originY, xAxisX, xAxisY, 0xFF5555FF, 2.0f);
        drawList.addText(xAxisX + 5, xAxisY - 5, 0xFF5555FF, "X");

        // Y轴
        float yAxisX = projectX(0, 5, 0, scale, centerX);
        float yAxisY = projectY(0, 5, 0, scale, centerY);
        drawList.addLine(originX, originY, yAxisX, yAxisY, 0xFF55FF55, 2.0f);
        drawList.addText(yAxisX - 5, yAxisY - 15, 0xFF55FF55, "Y");

        // Z轴
        float zAxisX = projectX(0, 0, 5, scale, centerX);
        float zAxisY = projectY(0, 0, 5, scale, centerY);
        drawList.addLine(originX, originY, zAxisX, zAxisY, 0xFFFF5555, 2.0f);
        drawList.addText(zAxisX - 15, zAxisY + 5, 0xFFFF5555, "Z");

        List<Vec3d> path = data.motionPreviewPath;
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d p1 = path.get(i);
            Vec3d p2 = path.get(i + 1);

            float x1 = projectX(p1.x, p1.y, p1.z, scale, centerX);
            float y1 = projectY(p1.x, p1.y, p1.z, scale, centerY);

            float x2 = projectX(p2.x, p2.y, p2.z, scale, centerX);
            float y2 = projectY(p2.x, p2.y, p2.z, scale, centerY);

            if (isInBounds(x1, y1, startX, startY, w, h) || isInBounds(x2, y2, startX, startY, w, h)) {
                drawList.addLine(x1, y1, x2, y2, 0xFFFFFFFF, 2.0f);
            }

            if (i % 5 == 0 && isInBounds(x1, y1, startX, startY, w, h)) {
                drawList.addCircleFilled(x1, y1, 2.0f, 0xFF00FFFF);
            }
        }
    }

    private static float projectX(double x, double y, double z, float scale, float centerX) {
        return (float) (centerX + (x - z) * 0.866f * scale);
    }

    private static float projectY(double x, double y, double z, float scale, float centerY) {
        return (float) (centerY - (y * scale) + (x + z) * 0.5f * scale);
    }

    private static boolean isInBounds(float x, float y, float sx, float sy, float w, float h) {
        return x >= sx && x <= sx + w && y >= sy && y <= sy + h;
    }
}