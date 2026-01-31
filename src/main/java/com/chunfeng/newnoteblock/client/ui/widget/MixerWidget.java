package com.chunfeng.newnoteblock.client.ui.widget;

import com.chunfeng.newnoteblock.audio.data.FilterDefinition;
import com.chunfeng.newnoteblock.audio.data.ReverbDefinition;
import com.chunfeng.newnoteblock.client.ui.data.NoteData;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;

public class MixerWidget {

    public static void render(NoteData data, Runnable onDataChange) {
        if (ImGui.beginTable("MixerLayout", 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.Resizable)) {

            ImGui.tableSetupColumn("Reverb", ImGuiTableColumnFlags.WidthStretch, 5.0f);
            ImGui.tableSetupColumn("EQ", ImGuiTableColumnFlags.WidthStretch, 1.0f);

            // ===========================
            // 1. Reverb
            // ===========================
            ImGui.tableNextColumn();

            float availWidth = ImGui.getContentRegionAvailX();
            float uniformSliderWidth = (availWidth / 5.0f) - ImGui.getStyle().getItemSpacingX();

            renderCenteredText("混响效果器(湿声)");
            ImGui.spacing();

            float topGroupWidth = (uniformSliderWidth * 2) + ImGui.getStyle().getItemSpacingX();
            float currentX = ImGui.getCursorPosX();
            float startX = currentX + (availWidth - topGroupWidth) / 2.0f;

            ImGui.setCursorPosX(startX);
            ImGui.pushItemWidth(uniformSliderWidth);
            if (ImGui.sliderFloat("##Send", data.reverbSend, 0.0f, 1.0f, "混响浓度: %.2f")) {
                if (onDataChange != null) onDataChange.run();
            }
            ImGui.popItemWidth();

            ImGui.sameLine();

            ImGui.pushItemWidth(uniformSliderWidth);
            String previewValue = "预设选择";
            if (data.currentPresetIndex.get() >= 0 &&
                    data.currentPresetIndex.get() < ReverbDefinition.PRESET_NAMES.length) {
                previewValue = ReverbDefinition.PRESET_NAMES[data.currentPresetIndex.get()];
            }
            if (ImGui.beginCombo("##Preset", previewValue)) {
                for (int i = 0; i < ReverbDefinition.PRESET_NAMES.length; i++) {
                    boolean isSelected = (data.currentPresetIndex.get() == i);
                    if (ImGui.selectable(ReverbDefinition.PRESET_NAMES[i], isSelected)) {
                        data.currentPresetIndex.set(i);
                        float[] p = ReverbDefinition.PRESETS.get(ReverbDefinition.PRESET_NAMES[i]);
                        if (p != null) {
                            System.arraycopy(p, 0, data.reverbParams, 0, Math.min(p.length, data.reverbParams.length));
                            if (onDataChange != null) onDataChange.run();
                        }
                    }
                    if (isSelected) ImGui.setItemDefaultFocus();
                }
                ImGui.endCombo();
            }
            ImGui.popItemWidth();

            ImGui.spacing();
            ImGui.separator();
            ImGui.text("详细参数:");

            renderReverbRow("Row1", 5, uniformSliderWidth, data, onDataChange,
                    new String[]{"密度", "扩散", "主增益", "高频增益", "低频增益"},
                    new int[]{ReverbDefinition.DENSITY, ReverbDefinition.DIFFUSION, ReverbDefinition.GAIN, ReverbDefinition.GAIN_HF, ReverbDefinition.GAIN_LF}
            );

            renderReverbRow("Row2", 3, uniformSliderWidth, data, onDataChange,
                    new String[]{"衰减时间", "高频衰减比", "低频衰减比"},
                    new int[]{ReverbDefinition.DECAY_TIME, ReverbDefinition.DECAY_HF_RATIO, ReverbDefinition.DECAY_LF_RATIO}
            );

            renderReverbRow("Row3", 5, uniformSliderWidth, data, onDataChange,
                    new String[]{"反射增益", "反射延迟", "反射声像X", "反射声像Y", "反射声像Z"},
                    new int[]{ReverbDefinition.REFLECTIONS_GAIN, ReverbDefinition.REFLECTIONS_DELAY, ReverbDefinition.REFLECTIONS_PAN_X, ReverbDefinition.REFLECTIONS_PAN_Y, ReverbDefinition.REFLECTIONS_PAN_Z}
            );

            renderReverbRow("Row4", 5, uniformSliderWidth, data, onDataChange,
                    new String[]{"后期混响增益", "后期混响延迟", "后期混响声像X", "后期混响声像Y", "后期混响声像Z"},
                    new int[]{ReverbDefinition.LATE_REVERB_GAIN, ReverbDefinition.LATE_REVERB_DELAY, ReverbDefinition.LATE_REVERB_PAN_X, ReverbDefinition.LATE_REVERB_PAN_Y, ReverbDefinition.LATE_REVERB_PAN_Z}
            );

            renderReverbRow("Row5", 5, uniformSliderWidth, data, onDataChange,
                    new String[]{"回声时间", "回声深度", "调制时间", "调制深度", "空气吸收高频增益"},
                    new int[]{ReverbDefinition.ECHO_TIME, ReverbDefinition.ECHO_DEPTH, ReverbDefinition.MODULATION_TIME, ReverbDefinition.MODULATION_DEPTH, ReverbDefinition.AIR_ABSORPTION_GAIN_HF}
            );

            renderReverbRow("Row6", 4, uniformSliderWidth, data, onDataChange,
                    new String[]{"高频参考值", "低频参考值", "房间衰减因子", "高频衰减限制"},
                    new int[]{ReverbDefinition.HF_REFERENCE, ReverbDefinition.LF_REFERENCE, ReverbDefinition.ROOM_ROLLOFF_FACTOR, ReverbDefinition.DECAY_HF_LIMIT}
            );


            // ===========================
            // 2. EQ
            // ===========================
            ImGui.tableNextColumn();

            renderCenteredText("均衡器(EQ)");
            ImGui.spacing();

            ImGui.pushItemWidth(-1);

            ImGui.textDisabled("低频 (Low)");
            sliderEqParam(data, onDataChange, "L-Freq", FilterDefinition.LOW_FREQ, 20.0f, 1000.0f, "%.0f Hz");
            sliderEqParam(data, onDataChange, "L-Gain", FilterDefinition.LOW_GAIN, -15.0f, 15.0f, "%.1f dB");
            sliderEqParam(data, onDataChange, "L-Q", FilterDefinition.LOW_Q, 0.1f, 2.0f, "Q: %.2f");
            ImGui.separator();

            ImGui.textDisabled("中频 (Mid)");
            sliderEqParam(data, onDataChange, "M-Gain", FilterDefinition.MID_GAIN, -15.0f, 15.0f, "%.1f dB");
            sliderEqParam(data, onDataChange, "M-Q", FilterDefinition.MID_Q, 0.1f, 10.0f, "Q: %.2f");
            ImGui.separator();

            ImGui.textDisabled("高频 (High)");
            sliderEqParam(data, onDataChange, "H-Freq", FilterDefinition.HIGH_FREQ, 1000.0f, 20000.0f, "%.0f Hz");
            sliderEqParam(data, onDataChange, "H-Gain", FilterDefinition.HIGH_GAIN, -15.0f, 15.0f, "%.1f dB");
            sliderEqParam(data, onDataChange, "H-Q", FilterDefinition.HIGH_Q, 0.1f, 2.0f, "Q: %.2f");

            ImGui.popItemWidth();

            ImGui.endTable();
        }
    }

    private static void sliderEqParam(NoteData data, Runnable onChange, String label, int idx, float min, float max, String fmt) {
        float[] val = {data.eqParams[idx]};
        if (ImGui.sliderFloat(label + "##EQ" + idx, val, min, max, fmt)) {
            data.eqParams[idx] = val[0];
            if (onChange != null) onChange.run();
        }
    }

    private static void renderCenteredText(String text) {
        ImVec2 textSize = new ImVec2();
        ImGui.calcTextSize(textSize, text);
        float textWidth = textSize.x;
        float columnWidth = ImGui.getContentRegionAvailX();
        float currentX = ImGui.getCursorPosX();
        if (columnWidth > textWidth) {
            ImGui.setCursorPosX(currentX + (columnWidth - textWidth) / 2.0f);
        }
        ImGui.text(text);
    }

    private static void renderReverbRow(String id, int cols, float width, NoteData data, Runnable onChange, String[] labels, int[] indices) {
        if (ImGui.beginTable("Reverb" + id, cols)) {
            for (int i = 0; i < indices.length; i++) {
                ImGui.tableNextColumn();
                ImGui.pushItemWidth(width);
                sliderParam(data, onChange, labels[i], indices[i]);
                ImGui.popItemWidth();
            }
            ImGui.endTable();
        }
    }

    private static void sliderParam(NoteData data, Runnable onChange, String label, int idx) {
        float[] val = {data.reverbParams[idx]};
        float min = 0.0f;
        float max = 1.0f;

        switch (idx) {
            case ReverbDefinition.DECAY_TIME: min=0.1f; max=20.0f; break;
            case ReverbDefinition.DECAY_HF_RATIO:
            case ReverbDefinition.DECAY_LF_RATIO: min=0.1f; max=2.0f; break;
            case ReverbDefinition.REFLECTIONS_GAIN: max=3.16f; break;
            case ReverbDefinition.REFLECTIONS_DELAY: max=0.3f; break;
            case ReverbDefinition.LATE_REVERB_GAIN: max=10.0f; break;
            case ReverbDefinition.LATE_REVERB_DELAY: max=0.1f; break;
            case ReverbDefinition.ECHO_TIME: min=0.075f; max=0.25f; break;
            case ReverbDefinition.MODULATION_TIME: min=0.04f; max=4.0f; break;
            case ReverbDefinition.AIR_ABSORPTION_GAIN_HF: min=0.892f; max=1.0f; break;
            case ReverbDefinition.HF_REFERENCE: min=1000.0f; max=20000.0f; break;
            case ReverbDefinition.LF_REFERENCE: min=20.0f; max=1000.0f; break;
            case ReverbDefinition.ROOM_ROLLOFF_FACTOR: max=10.0f; break;
            case ReverbDefinition.REFLECTIONS_PAN_X:
            case ReverbDefinition.REFLECTIONS_PAN_Y:
            case ReverbDefinition.REFLECTIONS_PAN_Z:
            case ReverbDefinition.LATE_REVERB_PAN_X:
            case ReverbDefinition.LATE_REVERB_PAN_Y:
            case ReverbDefinition.LATE_REVERB_PAN_Z:
                min = -1.0f; max = 1.0f; break;
            case ReverbDefinition.DECAY_HF_LIMIT:
                boolean b = val[0] > 0.5f;
                if (ImGui.checkbox("##" + label, b)) {
                    data.reverbParams[idx] = b ? 0.0f : 1.0f;
                    if (onChange != null) onChange.run();
                }
                ImGui.sameLine();
                ImGui.text(label);
                return;
        }

        if (ImGui.sliderFloat("##" + label, val, min, max, label + ": %.2f")) {
            data.reverbParams[idx] = val[0];
            data.currentPresetIndex.set(-1);
            if (onChange != null) onChange.run();
        }
    }
}