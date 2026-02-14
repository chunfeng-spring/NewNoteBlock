package com.chunfeng.newnoteblock.client.ui.widget;

import com.chunfeng.newnoteblock.audio.manager.AudioAnalyzer;
import com.chunfeng.newnoteblock.audio.manager.ClientSoundManager;
import com.chunfeng.newnoteblock.client.ui.data.NoteData;
import com.chunfeng.newnoteblock.client.ui.framework.ImGuiImpl;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PianoWidget {
    private static final int TOTAL_NOTES = 128;
    private static final int TOTAL_WHITE_KEYS = 75;
    private static final List<UUID> activePreviewSounds = new ArrayList<>();

    /**
     * @param data         包含音高、乐器等信息的数据对象
     * @param onNoteChange 当点击琴键改变音高时的回调
     */
    public static void render(float width, float height, NoteData data, Runnable onNoteChange) {
        float startX = ImGui.getCursorScreenPosX();
        float startY = ImGui.getCursorScreenPosY();

        float whiteKeyWidth = width / (float) TOTAL_WHITE_KEYS;
        float whiteKeyHeight = height;
        float blackKeyWidth = whiteKeyWidth * 0.6f;
        float blackKeyHeight = whiteKeyHeight * 0.6f;

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0f, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0f, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Border, 0.0f, 0.0f, 0.0f, 1.0f);

        var drawList = ImGui.getWindowDrawList();
        int hoveredNote = getNoteAtMousePosition(whiteKeyWidth, whiteKeyHeight, blackKeyWidth, blackKeyHeight, startX,
                startY);

        float currentWhiteX = startX;
        for (int i = 0; i < TOTAL_NOTES; i++) {
            if (!isBlackKey(i)) {
                float x2 = currentWhiteX + whiteKeyWidth;
                float y2 = startY + whiteKeyHeight;
                // 使用 data.note
                int color = (i == data.note) ? 0xFFC1B6FF : (i == hoveredNote ? 0xFFCCCCCC : 0xFFF2F2F2);
                drawList.addRectFilled(currentWhiteX, startY, x2, y2, color);
                drawList.addRect(currentWhiteX, startY, x2, y2, 0xFF000000);
                currentWhiteX += whiteKeyWidth;
            }
        }

        float blackKeyOffset = startX;
        for (int i = 0; i < TOTAL_NOTES; i++) {
            if (isBlackKey(i)) {
                float x1 = blackKeyOffset - (blackKeyWidth / 2.0f);
                float x2 = x1 + blackKeyWidth;
                float y2 = startY + blackKeyHeight;
                int color = (i == data.note) ? 0xFFC1B6FF : (i == hoveredNote ? 0xFF4D4D4D : 0xFF1A1A1A);
                drawList.addRectFilled(x1, startY, x2, y2, color);
                drawList.addRect(x1, startY, x2, y2, 0xFF000000);
            } else {
                blackKeyOffset += whiteKeyWidth;
            }
        }

        ImFont smallFont = ImGuiImpl.INSTANCE.fontSmall;
        boolean smallPushed = false;
        if (smallFont != null) {
            ImGui.pushFont(smallFont);
            smallPushed = true;
        }

        currentWhiteX = startX;
        for (int i = 0; i < TOTAL_NOTES; i++) {
            if (!isBlackKey(i)) {
                if (i % 12 == 0) {
                    int octave = (i / 12) - 1;
                    String label = "C" + octave;
                    ImVec2 textSize = new ImVec2();
                    ImGui.calcTextSize(textSize, label);
                    float textX = currentWhiteX + (whiteKeyWidth - textSize.x) / 2.0f;
                    float textY = startY + whiteKeyHeight - textSize.y - 5.0f;
                    drawList.addText(textX, textY, ImGui.getColorU32(0, 0, 0, 255), label);
                }
                currentWhiteX += whiteKeyWidth;
            }
        }
        if (smallPushed)
            ImGui.popFont();

        if (ImGui.isMouseClicked(0) && hoveredNote != -1) {
            data.note = hoveredNote;
            if (onNoteChange != null)
                onNoteChange.run();
            // 播放预览
            playPreviewSound(data);
        }

        ImGui.popStyleColor(1);
        ImGui.popStyleVar(3);
        ImGui.dummy(width, height);
    }

    private static void playPreviewSound(NoteData data) {
        int startTick = data.motionStartTick.get();
        int endTick = data.motionEndTick.get();

        List<Vec3d> previewPath = new ArrayList<>();
        if (startTick <= endTick && (endTick - startTick) <= 2000) {
            previewPath = com.chunfeng.newnoteblock.util.MotionCalculator.calculate(
                    data.motionExpX.get(),
                    data.motionExpY.get(),
                    data.motionExpZ.get(),
                    startTick, endTick);
        }

        BlockPos soundPos = BlockPos.ORIGIN;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            soundPos = client.player.getBlockPos();
        }

        UUID soundId = UUID.randomUUID();
        activePreviewSounds.add(soundId);

        ClientSoundManager.startSoundWithEnvelope(
                soundId,
                soundPos,
                data.instrument,
                data.note,
                data.volume.get(), // 主音量
                data.volumeCurve, // 音量曲线
                data.pitchCurve, // 音高曲线
                data.pitchRange.get(),
                data.reverbSend[0],
                data.reverbParams,
                data.eqParams,
                startTick,
                endTick,
                data.motionMode.get(),
                previewPath);
        AudioAnalyzer.updateSpectrumAsync(data.instrument, data.note, data.pitchRange.get());
    }

    private static int getNoteAtMousePosition(float wW, float wH, float bW, float bH, float startX, float startY) {
        float mouseX = ImGui.getMousePosX();
        float mouseY = ImGui.getMousePosY();
        if (mouseY >= startY && mouseY <= startY + bH) {
            float offset = startX;
            for (int i = 0; i < TOTAL_NOTES; i++) {
                if (isBlackKey(i)) {
                    float left = offset - (bW / 2.0f);
                    if (mouseX >= left && mouseX <= left + bW)
                        return i;
                } else {
                    offset += wW;
                }
            }
        }
        if (mouseY >= startY && mouseY <= startY + wH) {
            float relX = mouseX - startX;
            if (relX >= 0) {
                int index = (int) (relX / wW);
                if (index >= 0 && index < TOTAL_WHITE_KEYS)
                    return mapWhiteIndexToNote(index);
            }
        }
        return -1;
    }

    private static int mapWhiteIndexToNote(int whiteIndex) {
        int current = 0;
        for (int i = 0; i < TOTAL_NOTES; i++) {
            if (!isBlackKey(i)) {
                if (current == whiteIndex)
                    return i;
                current++;
            }
        }
        return -1;
    }

    private static boolean isBlackKey(int index) {
        int n = index % 12;
        return n == 1 || n == 3 || n == 6 || n == 8 || n == 10;
    }

    public static String getNoteName(int index) {
        String[] names = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
        int octave = (index / 12) - 1;
        return names[index % 12] + octave;
    }

    public static void stopAllPreviews() {
        for (UUID id : activePreviewSounds) {
            ClientSoundManager.stopSound(id);
        }
        activePreviewSounds.clear();
    }
}