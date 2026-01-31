package com.chunfeng.newnoteblock;

import com.chunfeng.newnoteblock.audio.engine.NoteBlockAudioEngine;
import com.chunfeng.newnoteblock.audio.manager.SamplerManager;
import com.chunfeng.newnoteblock.network.WEPacketHandler; // 确保导入这个
import com.chunfeng.newnoteblock.network.NotePacketHandler;
import com.chunfeng.newnoteblock.client.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents; // 导入 ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NewNoteBlockClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 1. 注册按键绑定
        KeyBindings.register();

        NotePacketHandler.registerClientPackets();
        WEPacketHandler.registerClientPackets();

        // --- 注册按键监听 ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.openWEGuiKey.wasPressed()) {
                // 如果当前没有打开其他界面，则请求打开批量编辑 GUI
                if (client.currentScreen == null) {
                    WEPacketHandler.requestOpenGui();
                }
            }
        });

        // --- HUD 渲染 ---
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && !client.options.hudHidden) {
                int used = NoteBlockAudioEngine.getUsedSlotCount();
                int max = NoteBlockAudioEngine.getMaxSlots();
                if (max > 0) {
                    String text = used + " / " + max;
                    TextRenderer tr = client.textRenderer;
                    int screenWidth = client.getWindow().getScaledWidth();
                    int x = screenWidth - tr.getWidth(text) - 5;
                    int y = 5;
                    drawContext.drawTextWithShadow(tr, text, x, y, 0xFFFFFF);
                }
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            NoteBlockAudioEngine.ensureInitialized();
            int slots = NoteBlockAudioEngine.getMaxSlots();
            if (client.player != null) {
                String status = slots > 0 ? "Loaded (" + slots + " slots)" : "Not Supported";
                Formatting color = slots > 0 ? Formatting.GREEN : Formatting.RED;
                client.player.sendMessage(Text.literal("NewNoteBlock Audio Engine: " + status).formatted(color),
                        false);
            }
        });

        SamplerManager.init();
        SamplerManager.forceRefresh();

        System.out.println("Client initialized.");
    }
}