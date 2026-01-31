package com.chunfeng.newnoteblock.client.ui.data;

import com.chunfeng.newnoteblock.network.WEPacketHandler;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;

import java.util.ArrayList;
import java.util.List;

/**
 * 集中管理 WorldEditScreen 的所有状态变量
 * (原 WorldEditState，已重命名并归入 data 包)
 */
public class WorldEditData {
    public static boolean isOpen = false;
    public static long selectionVolume = 0;

    // 独立的 NoteData 实例，用于存储 WorldEdit 编辑器中的“目标属性”
    public static final NoteData noteData = new NoteData();

    // --- 动态规则列表 ---
    public static final List<RuleEntry> filterRules = new ArrayList<>();

    // 临时状态 (Filter Builder)
    public static final ImInt newRuleType = new ImInt(0);
    public static final ImInt newRuleLogic = new ImInt(0);
    public static final ImBoolean newRuleInvert = new ImBoolean(false);
    public static final ImInt newRuleCatIndex = new ImInt(0);
    public static final ImInt newRuleInstIndex = new ImInt(0);
    public static final ImInt newRuleNoteMin = new ImInt(0);
    public static final ImInt newRuleNoteMax = new ImInt(24);

    // --- 模块开关 ---
    public static final ImBoolean enableTargetEditor = new ImBoolean(false);
    public static final ImBoolean enableOtherOps = new ImBoolean(false);

    // --- Mask (绝对值) ---
    public static final ImBoolean maskInstrument = new ImBoolean(false);
    public static final ImBoolean maskNote = new ImBoolean(false);
    public static final ImBoolean maskVolCurve = new ImBoolean(false);
    public static final ImBoolean maskPitchCurve = new ImBoolean(false);
    public static final ImBoolean maskPitchRange = new ImBoolean(false);
    public static final ImBoolean maskDelay = new ImBoolean(false);
    public static final ImBoolean maskReverb = new ImBoolean(false);
    public static final ImBoolean maskEq = new ImBoolean(false);
    public static final ImBoolean maskMotion = new ImBoolean(false);
    public static final ImBoolean maskMasterVolume = new ImBoolean(false); // [New]

    // --- 相对操作 (Shift) ---
    public static final ImFloat shiftNoteVal = new ImFloat(0.0f);
    public static final ImInt shiftNoteOp = new ImInt(0); // 0:+, 1:-, 2:*, 3:/

    public static final ImFloat shiftVolumeEnvVal = new ImFloat(0.0f); // [Renamed] Volume Envelope Offset
    public static final ImInt shiftVolumeEnvOp = new ImInt(0);

    public static final ImFloat shiftMasterVolumeVal = new ImFloat(0.0f); // [New] Master Volume Offset
    public static final ImInt shiftMasterVolumeOp = new ImInt(0);

    public static final ImFloat shiftDelayVal = new ImFloat(0.0f);
    public static final ImInt shiftDelayOp = new ImInt(0);

    public static final ImFloat shiftReverbVal = new ImFloat(0.0f);
    public static final ImInt shiftReverbOp = new ImInt(0);

    public static final ImFloat shiftReverbGainVal = new ImFloat(0.0f); // [New]
    public static final ImInt shiftReverbGainOp = new ImInt(0);

    public static final ImFloat shiftMotionXVal = new ImFloat(0.0f); // [New]
    public static final ImInt shiftMotionXOp = new ImInt(0);

    public static final ImFloat shiftMotionYVal = new ImFloat(0.0f); // [New]
    public static final ImInt shiftMotionYOp = new ImInt(0);

    public static final ImFloat shiftMotionZVal = new ImFloat(0.0f); // [New]
    public static final ImInt shiftMotionZOp = new ImInt(0);

    // 规则实体类
    public static class RuleEntry {
        public WEPacketHandler.FilterRule.Logic logic;
        public WEPacketHandler.FilterRule.Type type;
        public boolean invert;
        public String instrumentId;
        public String instrumentDisplayName;
        public int minNote, maxNote;

        public RuleEntry(WEPacketHandler.FilterRule.Logic l, boolean inv, String id, String name) {
            logic = l;
            invert = inv;
            type = WEPacketHandler.FilterRule.Type.INSTRUMENT;
            instrumentId = id;
            instrumentDisplayName = name;
        }

        public RuleEntry(WEPacketHandler.FilterRule.Logic l, boolean inv, int min, int max) {
            logic = l;
            invert = inv;
            type = WEPacketHandler.FilterRule.Type.NOTE;
            minNote = min;
            maxNote = max;
        }
    }

    public static void reset() {
        // 重置 NoteData
        noteData.resetToDefault();

        filterRules.clear();
        enableTargetEditor.set(false);
        enableOtherOps.set(false);
        setAllMasks(false);
        maskMasterVolume.set(false);

        shiftNoteVal.set(0.0f);
        shiftNoteOp.set(0);

        shiftVolumeEnvVal.set(0.0f);
        shiftVolumeEnvOp.set(0);

        shiftMasterVolumeVal.set(0.0f);
        shiftMasterVolumeOp.set(0);

        shiftDelayVal.set(0.0f);
        shiftDelayOp.set(0);

        shiftReverbVal.set(0.0f);
        shiftReverbOp.set(0);

        shiftReverbVal.set(0.0f);
        shiftReverbOp.set(0);

        shiftReverbGainVal.set(0.0f);
        shiftReverbGainOp.set(0);

        shiftMotionXVal.set(0.0f);
        shiftMotionXOp.set(0);
        shiftMotionYVal.set(0.0f);
        shiftMotionYOp.set(0);
        shiftMotionZVal.set(0.0f);
        shiftMotionZOp.set(0);

        newRuleType.set(0);
        newRuleLogic.set(0);
        newRuleInvert.set(false);
        newRuleCatIndex.set(0);
        newRuleInstIndex.set(0);
        newRuleNoteMin.set(0);
        newRuleNoteMax.set(24);
    }

    public static void setAllMasks(boolean val) {
        maskInstrument.set(val);
        maskNote.set(val);
        maskVolCurve.set(val);
        maskPitchCurve.set(val);
        maskPitchRange.set(val);
        maskDelay.set(val);
        maskReverb.set(val);
        maskEq.set(val);
        maskMotion.set(val);
        maskMasterVolume.set(val);
    }
}