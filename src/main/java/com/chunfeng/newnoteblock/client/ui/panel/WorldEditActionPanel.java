package com.chunfeng.newnoteblock.client.ui.panel;

import com.chunfeng.newnoteblock.client.ui.data.WorldEditData;
import com.chunfeng.newnoteblock.client.ui.widget.MixerWidget;
import com.chunfeng.newnoteblock.client.ui.widget.MotionWidget;
import com.chunfeng.newnoteblock.client.ui.widget.PianoWidget;
import imgui.ImGui;

public class WorldEditActionPanel {

    public static void renderTargetEditorSection(float screenH) {
        ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "1. 勾选要覆盖的属性：");
        if (ImGui.beginTable("MaskGrid", 3)) {
            ImGui.tableNextColumn();
            ImGui.checkbox("音色", WorldEditData.maskInstrument);
            ImGui.tableNextColumn();
            ImGui.checkbox("音高", WorldEditData.maskNote);
            ImGui.tableNextColumn();
            ImGui.checkbox("主音量", WorldEditData.maskMasterVolume);
            ImGui.tableNextColumn();
            ImGui.checkbox("音量包络", WorldEditData.maskVolCurve);

            // [修改] 移除了单独的“弯音范围”复选框，逻辑将合并到“音高包络”中
            ImGui.tableNextColumn();
            ImGui.checkbox("音高包络 (+弯音范围)", WorldEditData.maskPitchCurve);

            ImGui.tableNextColumn();
            ImGui.checkbox("播放延迟", WorldEditData.maskDelay);
            ImGui.tableNextColumn();
            ImGui.checkbox("混响参数", WorldEditData.maskReverb);
            ImGui.tableNextColumn();
            ImGui.checkbox("EQ 参数", WorldEditData.maskEq);
            ImGui.tableNextColumn();
            ImGui.checkbox("声源移动", WorldEditData.maskMotion);

            ImGui.endTable();
        }
        ImGui.dummy(0, 5);
        if (ImGui.button("全选"))
            WorldEditData.setAllMasks(true);
        ImGui.sameLine();
        if (ImGui.button("清空"))
            WorldEditData.setAllMasks(false);

        ImGui.dummy(0, 10);
        ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "2. 配置属性：");

        float availW = ImGui.getContentRegionAvailX();
        float topH = screenH * 0.35f;
        float pianoH = screenH * 0.20f;
        float mixerH = screenH * 0.32f;
        float motionH = screenH * 0.35f;

        // 传入 WorldEditData.noteData
        NoteSettingsPanel.render(availW, topH, WorldEditData.noteData, null);
        ImGui.dummy(0, 5);

        if (ImGui.beginChild("PianoSection", availW, pianoH, false)) {
            String noteInfo = "基础音高: " + WorldEditData.noteData.note + " ("
                    + PianoWidget.getNoteName(WorldEditData.noteData.note) + ")";
            ImGui.text(noteInfo);
            // 传入 WorldEditData.noteData
            PianoWidget.render(ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY(), WorldEditData.noteData,
                    null);
        }
        ImGui.endChild();
        ImGui.dummy(0, 5);

        if (ImGui.beginChild("MixerSection", availW, mixerH, false)) {
            MixerWidget.render(WorldEditData.noteData, null);
        }
        ImGui.endChild();
        ImGui.dummy(0, 5);

        if (ImGui.beginChild("MotionSection", availW * 0.5f, motionH, true)) {
            MotionWidget.render(WorldEditData.noteData);
        }
        ImGui.endChild();
    }

    public static void renderOtherOpsSection() {
        ImGui.textColored(1.0f, 0.6f, 0.2f, 1.0f, "在原有数值基础上进行增减/乘除：");
        ImGui.dummy(0, 5);

        renderOperatorInput("音高 (半音)", WorldEditData.shiftNoteOp, WorldEditData.shiftNoteVal, 0.0f, 1.0f);
        ImGui.dummy(0, 5);

        renderOperatorInput("主音量 (0.0-1.0)", WorldEditData.shiftMasterVolumeOp, WorldEditData.shiftMasterVolumeVal,
                0.0f, 0.1f);
        ImGui.dummy(0, 5);

        renderOperatorInput("音量包络 (%)", WorldEditData.shiftVolumeEnvOp, WorldEditData.shiftVolumeEnvVal, 0.0f, 1.0f);
        ImGui.dummy(0, 5);

        renderOperatorInput("播放延迟 (ms)", WorldEditData.shiftDelayOp, WorldEditData.shiftDelayVal, 0.0f, 1.0f);
        ImGui.dummy(0, 5);

        renderOperatorInput("混响发送量 (0.0-1.0)", WorldEditData.shiftReverbOp, WorldEditData.shiftReverbVal, 0.0f, 0.1f);
        ImGui.dummy(0, 5);

        renderOperatorInput("混响主增益 (0.0-1.0)", WorldEditData.shiftReverbGainOp, WorldEditData.shiftReverbGainVal,
                0.0f, 0.1f);
        ImGui.dummy(0, 5);

        renderOperatorInput("声源 X 表达式", WorldEditData.shiftMotionXOp, WorldEditData.shiftMotionXVal, -10000.0f, 1.0f);
        renderOperatorInput("声源 Y 表达式", WorldEditData.shiftMotionYOp, WorldEditData.shiftMotionYVal, -10000.0f, 1.0f);
        renderOperatorInput("声源 Z 表达式", WorldEditData.shiftMotionZOp, WorldEditData.shiftMotionZVal, -10000.0f, 1.0f);

        ImGui.dummy(0, 5);
    }

    private static final String[] OPS = { "+", "-", "x", "÷" };

    private static void renderOperatorInput(String label, imgui.type.ImInt op, imgui.type.ImFloat val,
            float minConstraint, float step) {
        ImGui.pushID(label);

        // Operator Selector (Combo)
        ImGui.setNextItemWidth(70);
        if (ImGui.beginCombo("##Op", OPS[op.get()])) {
            for (int i = 0; i < OPS.length; i++) {
                boolean isSelected = (op.get() == i);
                if (ImGui.selectable(OPS[i], isSelected)) {
                    op.set(i);
                }
                if (isSelected) {
                    ImGui.setItemDefaultFocus();
                }
            }
            ImGui.endCombo();
        }

        ImGui.sameLine();

        // Value Input
        ImGui.setNextItemWidth(180);
        // Use %.2f for decimals.
        ImGui.inputFloat("##Val", val, step, step * 10.0f, "%.2f");

        // "不能输入负数" -> Constraint: val >= 0 (or minConstraint)
        if (val.get() < minConstraint)
            val.set(minConstraint);

        ImGui.sameLine();
        ImGui.text(label);

        ImGui.popID();
    }
}