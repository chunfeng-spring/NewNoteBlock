package com.chunfeng.newnoteblock.client.ui.screen;

import com.chunfeng.newnoteblock.client.ui.data.WorldEditData;
import com.chunfeng.newnoteblock.client.ui.framework.ImGuiImpl;
import com.chunfeng.newnoteblock.client.ui.panel.NoteSettingsPanel;
import com.chunfeng.newnoteblock.client.ui.panel.WorldEditActionPanel;
import com.chunfeng.newnoteblock.client.ui.panel.WorldEditFilterPanel;
import com.chunfeng.newnoteblock.client.ui.widget.MotionWidget;
import com.chunfeng.newnoteblock.network.WEPacketHandler;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class WorldEditScreen {

    public static void open(long volume) {
        WorldEditData.isOpen = true;
        WorldEditData.selectionVolume = volume;

        // 使用 WorldEditData 的 noteData
        NoteSettingsPanel.updateInstrumentList(WorldEditData.noteData);
        NoteSettingsPanel.updateInstrumentFromSelection(WorldEditData.noteData);

        if (WorldEditData.filterRules.isEmpty() && !WorldEditData.enableTargetEditor.get()
                && !WorldEditData.enableOtherOps.get()) {
            WorldEditData.reset();
        }

        MinecraftClient.getInstance().setScreen(new Screen(Text.of("WE Editor")) {
            @Override
            public void close() {
                super.close();
                WorldEditData.isOpen = false;
            }

            @Override
            public void removed() {
                super.removed();
                WorldEditData.isOpen = false;
            }
        });
    }

    public static void render() {
        if (!WorldEditData.isOpen || ImGuiImpl.INSTANCE == null)
            return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            WorldEditData.isOpen = false;
            return;
        }

        var viewport = ImGui.getMainViewport();
        float screenW = viewport.getWorkSizeX();
        float screenH = viewport.getWorkSizeY();

        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY());
        ImGui.setNextWindowSize(screenW, screenH);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 10, 10);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.05f, 0.05f, 0.08f, 0.75f);

        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoCollapse;

        if (ImGui.begin("WEFullEditor", flags)) {
            renderHeader(screenW);
            ImGui.separator();
            renderControlGrid();

            ImGui.dummy(0, 5);
            ImGui.separator();

            ImGui.checkbox("绝对值覆盖", WorldEditData.enableTargetEditor);
            if (WorldEditData.enableTargetEditor.get()) {
                ImGui.indent();
                WorldEditActionPanel.renderTargetEditorSection(screenH);
                ImGui.unindent();
            }

            ImGui.dummy(0, 10);
            ImGui.separator();

            ImGui.checkbox("相对值偏移", WorldEditData.enableOtherOps);
            if (WorldEditData.enableOtherOps.get()) {
                ImGui.indent();
                WorldEditActionPanel.renderOtherOpsSection();
                ImGui.unindent();
            }
        }
        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar();
    }

    private static void renderHeader(float width) {
        ImGui.beginGroup();
        ImFont titleFont = ImGuiImpl.INSTANCE.fontTitle;
        if (titleFont != null)
            ImGui.pushFont(titleFont);

        String title = "WorldEdit GUI";
        ImVec2 titleSize = new ImVec2();
        ImGui.calcTextSize(titleSize, title);
        ImGui.setCursorPosX((width - titleSize.x) / 2.0f);
        ImGui.text(title);

        if (titleFont != null)
            ImGui.popFont();

        ImGui.setCursorPosX(10);
        ImGui.textColored(0.7f, 0.7f, 0.7f, 1.0f, "选区总方块数: " + WorldEditData.selectionVolume);

        float btnW = 80;
        ImGui.sameLine(width - btnW - 20);
        if (ImGui.button("关闭", btnW, 0)) {
            MinecraftClient.getInstance().setScreen(null);
        }
        ImGui.endGroup();
    }

    private static void renderControlGrid() {
        if (ImGui.beginTable("ControlGrid", 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.Resizable)) {
            ImGui.tableSetupColumn("Filters", ImGuiTableColumnFlags.WidthStretch, 0.75f);
            ImGui.tableSetupColumn("Actions", ImGuiTableColumnFlags.WidthStretch, 0.25f);

            // Col 1: Filters
            ImGui.tableNextColumn();
            WorldEditFilterPanel.render();

            // Col 2: Actions
            ImGui.tableNextColumn();
            ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "2. 执行操作");
            ImGui.dummy(0, 10);
            float btnHeight = 50.0f;
            if (ImGui.button("应用修改", -1, btnHeight)) {
                sendUpdate();
                MinecraftClient.getInstance().setScreen(null);
            }
            ImGui.dummy(0, 5);
            if (ImGui.button("重置设置", -1, btnHeight)) {
                WorldEditData.reset();
            }
            ImGui.dummy(0, 5);
            if (ImGui.button("撤销 ( //undo )", -1, btnHeight)) {
                MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("undo");
            }
            ImGui.dummy(0, 5);
            if (ImGui.button("重做 ( //redo )", -1, btnHeight)) {
                MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("redo");
            }
            ImGui.endTable();
        }
    }

    private static void sendUpdate() {
        // 1. 构建 Filter
        WEPacketHandler.FilterOptions filter = new WEPacketHandler.FilterOptions();
        for (WorldEditData.RuleEntry entry : WorldEditData.filterRules) {
            if (entry.type == WEPacketHandler.FilterRule.Type.INSTRUMENT) {
                filter.rules.add(new WEPacketHandler.FilterRule(entry.logic, entry.invert, entry.instrumentId));
            } else {
                filter.rules
                        .add(new WEPacketHandler.FilterRule(entry.logic, entry.invert, entry.minNote, entry.maxNote));
            }
        }

        // 2. 构建 Mask
        WEPacketHandler.UpdateMask mask = new WEPacketHandler.UpdateMask();
        if (WorldEditData.enableTargetEditor.get()) {
            mask.updateInstrument = WorldEditData.maskInstrument.get();
            mask.updateNote = WorldEditData.maskNote.get();
            mask.updateVolumeCurve = WorldEditData.maskVolCurve.get();
            mask.updatePitchCurve = WorldEditData.maskPitchCurve.get();

            // [修改] 弯音范围的 Mask 跟随 音高包络
            mask.updatePitchRange = WorldEditData.maskPitchCurve.get();

            mask.updateDelay = WorldEditData.maskDelay.get();
            mask.updateReverb = WorldEditData.maskReverb.get();
            mask.updateEq = WorldEditData.maskEq.get();
            mask.updateMotion = WorldEditData.maskMotion.get();
            mask.updateMasterVolume = WorldEditData.maskMasterVolume.get(); // [New]

            // [New] Force update trajectory if motion is updated
            if (mask.updateMotion) {
                MotionWidget.generateTrajectory(WorldEditData.noteData);
            }
        }

        if (WorldEditData.enableOtherOps.get()) {
            mask.shiftNote = (WorldEditData.shiftNoteVal.get() != 0.0f || WorldEditData.shiftNoteOp.get() != 0);
            mask.shiftVolumeEnv = (WorldEditData.shiftVolumeEnvVal.get() != 0.0f
                    || WorldEditData.shiftVolumeEnvOp.get() != 0);
            mask.shiftMasterVolume = (WorldEditData.shiftMasterVolumeVal.get() != 0.0f
                    || WorldEditData.shiftMasterVolumeOp.get() != 0);
            mask.shiftDelay = (WorldEditData.shiftDelayVal.get() != 0.0f || WorldEditData.shiftDelayOp.get() != 0);
            mask.shiftReverbSend = (WorldEditData.shiftReverbVal.get() != 0.0f
                    || WorldEditData.shiftReverbOp.get() != 0);
            mask.shiftReverbGain = (WorldEditData.shiftReverbGainVal.get() != 0.0f
                    || WorldEditData.shiftReverbGainOp.get() != 0); // [New]

            mask.shiftMotionX = (WorldEditData.shiftMotionXVal.get() != 0.0f
                    || WorldEditData.shiftMotionXOp.get() != 0); // [New]
            mask.shiftMotionY = (WorldEditData.shiftMotionYVal.get() != 0.0f
                    || WorldEditData.shiftMotionYOp.get() != 0); // [New]
            mask.shiftMotionZ = (WorldEditData.shiftMotionZVal.get() != 0.0f
                    || WorldEditData.shiftMotionZOp.get() != 0); // [New]
        }

        // 3. 构建 Data (从 WorldEditData.noteData 读取)
        WEPacketHandler.DataPayload data = new WEPacketHandler.DataPayload();

        data.instrument = WorldEditData.noteData.instrument;
        data.note = WorldEditData.noteData.note;
        data.volume = WorldEditData.noteData.volume.get(); // [New]
        data.volCurve = new java.util.ArrayList<>(WorldEditData.noteData.volumeCurve);
        data.pitchCurve = new java.util.ArrayList<>(WorldEditData.noteData.pitchCurve);
        data.pitchRange = WorldEditData.noteData.pitchRange.get();
        data.delay = WorldEditData.noteData.delay.get();

        data.reverbSend = WorldEditData.noteData.reverbSend[0];
        data.reverbParams = WorldEditData.noteData.reverbParams.clone();
        data.eqParams = WorldEditData.noteData.eqParams.clone();

        data.expX = WorldEditData.noteData.motionExpX.get();
        data.expY = WorldEditData.noteData.motionExpY.get();
        data.expZ = WorldEditData.noteData.motionExpZ.get();
        data.startTick = WorldEditData.noteData.motionStartTick.get();
        data.startTick = WorldEditData.noteData.motionStartTick.get();
        data.endTick = WorldEditData.noteData.motionEndTick.get();
        data.motionMode = WorldEditData.noteData.motionMode.get(); // [New]
        data.motionPath = WorldEditData.noteData.motionPreviewPath;

        data.shiftNoteVal = WorldEditData.shiftNoteVal.get();
        data.shiftNoteOp = WorldEditData.shiftNoteOp.get();

        data.shiftVolumeEnvVal = WorldEditData.shiftVolumeEnvVal.get(); // [Renamed]
        data.shiftVolumeEnvOp = WorldEditData.shiftVolumeEnvOp.get();

        data.shiftMasterVolumeVal = WorldEditData.shiftMasterVolumeVal.get(); // [New]
        data.shiftMasterVolumeOp = WorldEditData.shiftMasterVolumeOp.get();

        data.shiftDelayVal = WorldEditData.shiftDelayVal.get();
        data.shiftDelayOp = WorldEditData.shiftDelayOp.get();

        data.shiftReverbSendVal = WorldEditData.shiftReverbVal.get();
        data.shiftReverbOp = WorldEditData.shiftReverbOp.get();

        data.shiftReverbGainVal = WorldEditData.shiftReverbGainVal.get(); // [New]
        data.shiftReverbGainOp = WorldEditData.shiftReverbGainOp.get();

        data.shiftMotionXVal = WorldEditData.shiftMotionXVal.get(); // [New]
        data.shiftMotionXOp = WorldEditData.shiftMotionXOp.get();

        data.shiftMotionYVal = WorldEditData.shiftMotionYVal.get(); // [New]
        data.shiftMotionYOp = WorldEditData.shiftMotionYOp.get();

        data.shiftMotionZVal = WorldEditData.shiftMotionZVal.get(); // [New]
        data.shiftMotionZOp = WorldEditData.shiftMotionZOp.get();

        WEPacketHandler.sendWEUpdate(filter, mask, data);
    }
}