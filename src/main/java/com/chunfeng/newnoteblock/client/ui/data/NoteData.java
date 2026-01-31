package com.chunfeng.newnoteblock.client.ui.data;

import com.chunfeng.newnoteblock.audio.data.FilterDefinition;
import com.chunfeng.newnoteblock.audio.data.ReverbDefinition;
import com.chunfeng.newnoteblock.util.InstrumentBlockRegistry;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装所有音符盒可编辑的参数，消除静态状态污染。
 * 包含数据模型 (Model) 和部分 UI 状态 (ViewModel)。
 */
public class NoteData {
    // --- 核心数据 ---
    public int note = 60;
    public String instrument = "harp";
    public final ImFloat volume = new ImFloat(1.0f);

    public final ArrayList<Integer> volumeCurve = new ArrayList<>();
    public final ArrayList<Integer> pitchCurve = new ArrayList<>();

    public final ImInt tickCount = new ImInt(0);
    public final ImInt pitchRange = new ImInt(2);
    public final ImInt delay = new ImInt(0);

    public final float[] reverbSend = { 0.0f };
    public final float[] reverbParams = new float[ReverbDefinition.PARAM_COUNT];
    public final ImInt currentPresetIndex = new ImInt(-1);

    public final float[] eqParams = new float[FilterDefinition.PARAM_COUNT];

    public final ImString motionExpX = new ImString("0", 256);
    public final ImString motionExpY = new ImString("0", 256);
    public final ImString motionExpZ = new ImString("0", 256);
    public final ImInt motionStartTick = new ImInt(0);
    public final ImInt motionEndTick = new ImInt(40);
    public final ImBoolean motionMode = new ImBoolean(true); // [新增]

    public final List<Vec3d> motionPreviewPath = new ArrayList<>();

    public final ImInt currentCategoryIndex = new ImInt(0);
    public String[] currentInstrumentList = new String[0];
    public final ImInt currentInstrumentIndex = new ImInt(0);

    public NoteData() {
        resetToDefault();
    }

    public void resetToDefault() {
        note = 60;
        instrument = "harp";
        volume.set(1.0f);
        volumeCurve.clear();
        pitchCurve.clear();
        tickCount.set(0);
        pitchRange.set(2);
        delay.set(0);

        reverbSend[0] = 0.0f;
        System.arraycopy(ReverbDefinition.getDefault(), 0, reverbParams, 0, ReverbDefinition.PARAM_COUNT);
        currentPresetIndex.set(-1);

        System.arraycopy(FilterDefinition.getDefault(), 0, eqParams, 0, FilterDefinition.PARAM_COUNT);

        motionExpX.set("0");
        motionExpY.set("0");
        motionExpZ.set("0");
        motionStartTick.set(0);
        motionEndTick.set(40);
        motionMode.set(true); // Default Relative
        motionPreviewPath.clear();

        // 重置 UI 选择
        syncUIFromInstrument("harp");
    }

    public void copyFrom(NoteData other) {
        this.note = other.note;
        this.instrument = other.instrument;
        this.volume.set(other.volume.get());

        this.volumeCurve.clear();
        this.volumeCurve.addAll(other.volumeCurve);

        this.pitchCurve.clear();
        this.pitchCurve.addAll(other.pitchCurve);

        this.tickCount.set(other.tickCount.get());
        this.pitchRange.set(other.pitchRange.get());
        this.delay.set(other.delay.get());

        this.reverbSend[0] = other.reverbSend[0];
        System.arraycopy(other.reverbParams, 0, this.reverbParams, 0, ReverbDefinition.PARAM_COUNT);
        this.currentPresetIndex.set(other.currentPresetIndex.get());

        System.arraycopy(other.eqParams, 0, this.eqParams, 0, FilterDefinition.PARAM_COUNT);

        this.motionExpX.set(other.motionExpX.get());
        this.motionExpY.set(other.motionExpY.get());
        this.motionExpZ.set(other.motionExpZ.get());
        this.motionStartTick.set(other.motionStartTick.get());
        this.motionEndTick.set(other.motionEndTick.get());
        this.motionMode.set(other.motionMode.get());

        this.motionPreviewPath.clear();
        this.motionPreviewPath.addAll(other.motionPreviewPath);

        // 同步 UI 状态
        syncUIFromInstrument(this.instrument);
    }

    /**
     * 根据当前 instrument ID 反向更新 UI 的分类和索引
     */
    public void syncUIFromInstrument(String id) {
        if (id == null)
            id = "harp";
        for (int i = 0; i < InstrumentBlockRegistry.CATEGORIES.length; i++) {
            String cat = InstrumentBlockRegistry.CATEGORIES[i];
            LinkedHashMap<String, String> map = InstrumentBlockRegistry.UI_INSTRUMENT_MAP.get(cat);
            if (map == null)
                continue;

            int j = 0;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getValue().equals(id)) {
                    this.currentCategoryIndex.set(i);
                    updateInstrumentListInternal();
                    this.currentInstrumentIndex.set(j);
                    return;
                }
                j++;
            }
        }
        // Fallback
        updateInstrumentListInternal();
    }

    public void updateInstrumentListInternal() {
        String cat = InstrumentBlockRegistry.CATEGORIES[currentCategoryIndex.get()];
        LinkedHashMap<String, String> map = InstrumentBlockRegistry.UI_INSTRUMENT_MAP.get(cat);
        if (map != null) {
            this.currentInstrumentList = map.keySet().toArray(new String[0]);
        } else {
            this.currentInstrumentList = new String[0];
        }
    }

    public void updateInstrumentFromSelection() {
        String cat = InstrumentBlockRegistry.CATEGORIES[currentCategoryIndex.get()];
        if (currentInstrumentIndex.get() >= 0 && currentInstrumentIndex.get() < currentInstrumentList.length) {
            String displayName = currentInstrumentList[currentInstrumentIndex.get()];
            this.instrument = InstrumentBlockRegistry.UI_INSTRUMENT_MAP.get(cat).get(displayName);
        }
    }
}