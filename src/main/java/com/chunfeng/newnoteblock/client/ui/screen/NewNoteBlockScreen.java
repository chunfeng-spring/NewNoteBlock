package com.chunfeng.newnoteblock.client.ui.screen;

import com.chunfeng.newnoteblock.NewNoteBlockMod;
import com.chunfeng.newnoteblock.audio.data.FilterDefinition;
import com.chunfeng.newnoteblock.audio.data.ReverbDefinition;
import com.chunfeng.newnoteblock.block.NewNoteBlockEntity;
import com.chunfeng.newnoteblock.client.ui.data.NoteData;
import com.chunfeng.newnoteblock.client.ui.framework.ImGuiImpl;
import com.chunfeng.newnoteblock.client.ui.panel.NoteSettingsPanel;
import com.chunfeng.newnoteblock.client.ui.widget.GraphWidget;
import com.chunfeng.newnoteblock.client.ui.widget.MixerWidget;
import com.chunfeng.newnoteblock.client.ui.widget.MotionWidget;
import com.chunfeng.newnoteblock.client.ui.widget.PianoWidget;
import com.chunfeng.newnoteblock.util.InstrumentBlockRegistry;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImFloat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;

public class NewNoteBlockScreen {
    public static boolean isOpen = false;
    public static BlockPos currentPos = null;

    // [新增] 核心数据对象，独立于 WorldEditState
    public static final NoteData noteData = new NoteData();

    // 辅助计算器状态 (UI临时变量，无需存入 NoteData)
    public static final ImFloat calcTickRate = new ImFloat(20.0f);
    public static final ImFloat calcDelayTicks = new ImFloat(0.0f);

    // 频谱数据缓存 (全局共享，仅用于显示)
    public static final float[] cachedSpectrumData = new float[256];

    public static void openNoteBlockUI(BlockPos pos) {
        currentPos = pos;
        isOpen = true;

        // 重置数据
        noteData.resetToDefault();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            BlockEntity be = client.world.getBlockEntity(pos);
            if (be instanceof NewNoteBlockEntity noteBe) {
                // 填充数据
                noteData.note = noteBe.getNote();
                noteData.instrument = noteBe.getInstrument() == null ? "harp" : noteBe.getInstrument();
                noteData.volume.set(noteBe.getVolume());

                noteData.volumeCurve.clear();
                noteData.volumeCurve.addAll(noteBe.getVolumeCurve());

                noteData.pitchCurve.clear();
                noteData.pitchCurve.addAll(noteBe.getPitchCurve());

                noteData.tickCount.set(noteData.volumeCurve.size());
                noteData.pitchRange.set(noteBe.getPitchRange());
                noteData.delay.set(noteBe.getDelay());

                noteData.reverbSend[0] = noteBe.getReverbSend();
                float[] params = noteBe.getReverbParams();
                if (params != null && params.length == ReverbDefinition.PARAM_COUNT) {
                    System.arraycopy(params, 0, noteData.reverbParams, 0, ReverbDefinition.PARAM_COUNT);
                }
                noteData.currentPresetIndex.set(-1);

                float[] eParams = noteBe.getEqParams();
                if (eParams != null && eParams.length == FilterDefinition.PARAM_COUNT) {
                    System.arraycopy(eParams, 0, noteData.eqParams, 0, FilterDefinition.PARAM_COUNT);
                }

                noteData.motionExpX.set(noteBe.getMotionExpX() != null ? noteBe.getMotionExpX() : "0");
                noteData.motionExpY.set(noteBe.getMotionExpY() != null ? noteBe.getMotionExpY() : "0");
                noteData.motionExpZ.set(noteBe.getMotionExpZ() != null ? noteBe.getMotionExpZ() : "0");
                noteData.motionStartTick.set(noteBe.getMotionStartTick());
                noteData.motionEndTick.set(noteBe.getMotionEndTick());
                noteData.motionMode.set(noteBe.getMotionMode()); // [新增]

                // [关键] 初始化 UI 状态 (Instrument List & Index)
                syncUIFromInstrument(noteData);
            }
        }
    }

    // [新增] 使用服务端同步的数据直接打开 GUI，解决 WorldEdit 选区导致数据不同步的问题
    public static void openWithSyncedData(BlockPos pos, int note, String instrument, float volume,
            java.util.List<Integer> volCurve, java.util.List<Integer> pitchCurve,
            int pitchRange, int delay, float reverbSend, float[] rParams, float[] eParams,
            String expX, String expY, String expZ, int startTick, int endTick, boolean motionMode) {

        currentPos = pos;
        isOpen = true;

        // 使用服务端同步的数据填充
        noteData.resetToDefault();
        noteData.note = note;
        noteData.instrument = instrument != null ? instrument : "harp";
        noteData.volume.set(volume);

        noteData.volumeCurve.clear();
        if (volCurve != null)
            noteData.volumeCurve.addAll(volCurve);

        noteData.pitchCurve.clear();
        if (pitchCurve != null)
            noteData.pitchCurve.addAll(pitchCurve);

        noteData.tickCount.set(noteData.volumeCurve.size());
        noteData.pitchRange.set(pitchRange);
        noteData.delay.set(delay);

        noteData.reverbSend[0] = reverbSend;
        if (rParams != null && rParams.length == ReverbDefinition.PARAM_COUNT) {
            System.arraycopy(rParams, 0, noteData.reverbParams, 0, ReverbDefinition.PARAM_COUNT);
        }
        noteData.currentPresetIndex.set(-1);

        if (eParams != null && eParams.length == FilterDefinition.PARAM_COUNT) {
            System.arraycopy(eParams, 0, noteData.eqParams, 0, FilterDefinition.PARAM_COUNT);
        }

        noteData.motionExpX.set(expX != null ? expX : "0");
        noteData.motionExpY.set(expY != null ? expY : "0");
        noteData.motionExpZ.set(expZ != null ? expZ : "0");
        noteData.motionStartTick.set(startTick);
        noteData.motionEndTick.set(endTick);
        noteData.motionMode.set(motionMode);

        // 初始化 UI 状态
        syncUIFromInstrument(noteData);
    }

    public static void close() {
        if (isOpen && currentPos != null) {
            safeSendPacket();
        }
        isOpen = false;
        currentPos = null;
        GraphWidget.resetDragging();
        noteData.motionPreviewPath.clear();
        PianoWidget.stopAllPreviews();
        com.chunfeng.newnoteblock.audio.manager.AudioAnalyzer.stop();
    }

    public static void safeSendPacket() {
        try {
            if (currentPos == null)
                return;
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(currentPos);
            buf.writeInt(noteData.note);
            buf.writeString(noteData.instrument);
            buf.writeFloat(noteData.volume.get());

            buf.writeInt(noteData.volumeCurve.size());
            for (Integer val : noteData.volumeCurve)
                buf.writeInt(val);

            buf.writeInt(noteData.pitchCurve.size());
            for (Integer val : noteData.pitchCurve)
                buf.writeInt(val);

            buf.writeInt(noteData.pitchRange.get());
            buf.writeInt(noteData.delay.get());

            buf.writeFloat(noteData.reverbSend[0]);
            for (float p : noteData.reverbParams)
                buf.writeFloat(p);

            for (float p : noteData.eqParams)
                buf.writeFloat(p);

            buf.writeString(noteData.motionExpX.get());
            buf.writeString(noteData.motionExpY.get());
            buf.writeString(noteData.motionExpZ.get());
            buf.writeInt(noteData.motionStartTick.get());
            buf.writeInt(noteData.motionEndTick.get());
            buf.writeBoolean(noteData.motionMode.get()); // [新增]

            List<Vec3d> calculatedPath = calculateMotionPath();
            buf.writeInt(calculatedPath.size());
            for (Vec3d vec : calculatedPath) {
                buf.writeFloat((float) vec.x);
                buf.writeFloat((float) vec.y);
                buf.writeFloat((float) vec.z);
            }

            Identifier packetId = new Identifier(NewNoteBlockMod.MOD_ID, "update_note");
            ClientPlayNetworking.send(packetId, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Vec3d> calculateMotionPath() {
        int start = noteData.motionStartTick.get();
        int end = noteData.motionEndTick.get();
        return com.chunfeng.newnoteblock.util.MotionCalculator.calculate(
                noteData.motionExpX.get(),
                noteData.motionExpY.get(),
                noteData.motionExpZ.get(),
                start, end);
    }

    public static void render() {
        if (ImGuiImpl.INSTANCE == null)
            return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isOpen || currentPos == null || client.currentScreen == null)
            return;

        var viewport = ImGui.getMainViewport();
        float screenW = viewport.getWorkSizeX();
        float screenH = viewport.getWorkSizeY();

        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY());
        ImGui.setNextWindowSize(screenW, screenH);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, screenW * 0.02f, screenH * 0.02f);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.0f, 0.0f, 0.0f, 0.75f);

        int windowFlags = ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoSavedSettings;

        ImGui.begin("Editor", windowFlags);

        // Header
        {
            ImGui.beginGroup();
            ImFont titleFont = ImGuiImpl.INSTANCE.fontTitle;
            if (titleFont != null)
                ImGui.pushFont(titleFont);
            String titleText = "New NoteBlock Editor";
            float windowWidth = ImGui.getWindowWidth();
            ImVec2 titleSize = new ImVec2();
            ImGui.calcTextSize(titleSize, titleText);
            ImGui.setCursorPosX((windowWidth - titleSize.x) * 0.5f);
            ImGui.text(titleText);
            if (titleFont != null)
                ImGui.popFont();

            ImGui.sameLine();
            // [新增] 手持音符盒不打开GUI 切换按钮
            {
                boolean current = com.chunfeng.newnoteblock.client.config.ModConfig.get().skipGuiWhenHoldingNoteBlock;
                String toggleLabel = "手持音符盒不打开GUI：" + (current ? "ON" : "OFF");
                ImVec2 toggleSize = new ImVec2();
                ImGui.calcTextSize(toggleSize, toggleLabel);
                float toggleW = toggleSize.x + 20.0f;
                float closeBtnW = 100.0f;
                ImGui.setCursorPosX(windowWidth - closeBtnW - toggleW - ImGui.getStyle().getWindowPaddingX() - 10.0f);
                if (ImGui.button(toggleLabel, toggleW, titleSize.y * 0.5f)) {
                    com.chunfeng.newnoteblock.client.config.ModConfig.get().skipGuiWhenHoldingNoteBlock = !current;
                    com.chunfeng.newnoteblock.client.config.ModConfig.save();
                }
            }
            ImGui.sameLine();
            float btnW = 100.0f;
            ImGui.setCursorPosX(windowWidth - btnW - ImGui.getStyle().getWindowPaddingX());
            if (ImGui.button("关闭", btnW, titleSize.y * 0.5f))
                client.setScreen(null);
            ImGui.endGroup();
        }

        ImFont regFont = ImGuiImpl.INSTANCE.fontRegular;
        boolean regPushed = false;
        if (regFont != null) {
            ImGui.pushFont(regFont);
            regPushed = true;
        }

        float availW = ImGui.getContentRegionAvailX();
        float viewportH = ImGui.getMainViewport().getWorkSizeY();
        float topH = viewportH * 0.35f;
        float pianoH = viewportH * 0.20f;
        float mixerH = viewportH * 0.32f;
        float motionH = viewportH * 0.35f;

        // --- 顶部波形编辑 (传入 noteData) ---
        NoteSettingsPanel.render(availW, topH, noteData, NewNoteBlockScreen::safeSendPacket);

        // --- 钢琴卷帘 (传入 noteData) ---
        if (ImGui.beginChild("PianoSection", availW, pianoH, false)) {
            String noteInfo = "当前音高: " + noteData.note + " (" + PianoWidget.getNoteName(noteData.note) + ")";
            ImVec2 infoSize = new ImVec2();
            ImGui.calcTextSize(infoSize, noteInfo);
            ImGui.setCursorPosX((ImGui.getContentRegionAvailX() - infoSize.x) * 0.5f);
            ImGui.text(noteInfo);

            PianoWidget.render(ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY(), noteData,
                    NewNoteBlockScreen::safeSendPacket);
        }
        ImGui.endChild();

        // --- 混音器 (传入 noteData) ---
        if (ImGui.beginChild("MixerSection", availW, mixerH, false)) {
            MixerWidget.render(noteData, NewNoteBlockScreen::safeSendPacket);
        }
        ImGui.endChild();

        // --- 运动路径 (传入 noteData) ---
        ImGui.dummy(0.0f, 10.0f);
        if (ImGui.beginChild("MotionSection", availW * 0.5f, motionH, true)) {
            MotionWidget.render(noteData);
        }
        ImGui.endChild();

        if (regPushed)
            ImGui.popFont();
        ImGui.end();
        ImGui.popStyleColor(1);
        ImGui.popStyleVar(1);
    }

    // 辅助：根据 instrument 字符串反推分类和索引
    private static void syncUIFromInstrument(NoteData data) {
        String id = data.instrument;
        if (id == null)
            id = "harp";

        for (int i = 0; i < InstrumentBlockRegistry.CATEGORIES.length; i++) {
            String cat = InstrumentBlockRegistry.CATEGORIES[i];
            Map<String, String> map = InstrumentBlockRegistry.UI_INSTRUMENT_MAP.get(cat);
            if (map != null) {
                int j = 0;
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (entry.getValue().equals(id)) {
                        data.currentCategoryIndex.set(i);
                        NoteSettingsPanel.updateInstrumentList(data);
                        data.currentInstrumentIndex.set(j);
                        return;
                    }
                    j++;
                }
            }
        }
    }
}