package com.chunfeng.newnoteblock.audio.engine;

import com.chunfeng.newnoteblock.audio.data.ReverbDefinition;
import org.lwjgl.openal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NoteBlockAudioEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger("NoteBlockAudioEngine");

    private static int targetSlots = 128;
    private static int activeSlots = 0;

    private static int[] auxEffectSlots;
    private static int[] reverbEffects;
    // [删除] directFilters 数组
    private static int sharedFilter = 0;

    private static ParamSnapshot[] slotSnapshots;
    private static int[] slotRefCounts;
    private static long[] slotReleaseTime;

    private static final Map<Integer, Integer> sourceSlotMap = new ConcurrentHashMap<>();

    private static boolean initialized = false;
    private static boolean supported = false;
    private static long contextPointer = 0;

    public static final ConcurrentLinkedQueue<ReverbTask> taskQueue = new ConcurrentLinkedQueue<>();

    private static class ParamSnapshot {
        final float sendGain;
        final float[] params;
        final int hash;

        ParamSnapshot(float send, float[] p) {
            this.sendGain = send;
            this.params = p != null ? p.clone() : null;
            this.hash = Objects.hash(send, Arrays.hashCode(params));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ParamSnapshot that = (ParamSnapshot) o;
            return Float.compare(that.sendGain, sendGain) == 0 && Arrays.equals(params, that.params);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static class ReverbTask {
        public int sourceId;
        public final float[] params;
        public final float sendGain;
        // [修改] 移除了 filter 相关的参数，因为现在 EQ 在软件层处理

        public ReverbTask(int sourceId, float[] params, float sendGain) {
            this.sourceId = sourceId;
            this.params = params;
            this.sendGain = sendGain;
        }
    }

    public static void processTask(ReverbTask task) {
        ensureInitialized();
        if (!supported || activeSlots == 0)
            return;

        AL10.alGetError();
        try {
            // [删除] applyDirectFilter 调用

            if (task.sendGain <= 0.01f) {
                disconnect(task.sourceId);
            } else {
                if (sourceSlotMap.containsKey(task.sourceId)) {
                    disconnect(task.sourceId);
                }

                int slotIndex = findOrAllocateSlot(task.sendGain, task.params);
                int slotId = auxEffectSlots[slotIndex];

                EXTEfx.alFilterf(sharedFilter, EXTEfx.AL_LOWPASS_GAIN, task.sendGain);
                EXTEfx.alFilterf(sharedFilter, EXTEfx.AL_LOWPASS_GAINHF, 1.0f);
                AL11.alSource3i(task.sourceId, EXTEfx.AL_AUXILIARY_SEND_FILTER, slotId, 0, sharedFilter);

                sourceSlotMap.put(task.sourceId, slotIndex);
                slotRefCounts[slotIndex]++;
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public static void disconnect(int sourceId) {
        ensureInitialized();
        if (!supported)
            return;
        try {
            AL11.alSource3i(sourceId, EXTEfx.AL_AUXILIARY_SEND_FILTER, 0, 0, EXTEfx.AL_FILTER_NULL);
            // [删除] AL_DIRECT_FILTER 断开逻辑 (默认为 NULL 即可)
            AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);

            Integer slotIndex = sourceSlotMap.remove(sourceId);
            if (slotIndex != null && slotIndex >= 0 && slotIndex < activeSlots) {
                slotRefCounts[slotIndex]--;
                if (slotRefCounts[slotIndex] <= 0) {
                    slotRefCounts[slotIndex] = 0;
                    slotReleaseTime[slotIndex] = System.currentTimeMillis();
                }
            }
        } catch (Exception ignored) {
        }
    }

    // --- 内部逻辑 (保持不变，除了 cleanup) ---

    private static int findOrAllocateSlot(float sendGain, float[] params) {
        ParamSnapshot newSnap = new ParamSnapshot(sendGain, params);
        for (int i = 0; i < activeSlots; i++) {
            if (slotRefCounts[i] > 0 && slotSnapshots[i] != null && slotSnapshots[i].equals(newSnap)) {
                return i;
            }
        }
        int bestIndex = -1;
        long oldestTime = Long.MAX_VALUE;
        for (int i = 0; i < activeSlots; i++) {
            if (slotRefCounts[i] == 0) {
                if (slotReleaseTime[i] < oldestTime) {
                    oldestTime = slotReleaseTime[i];
                    bestIndex = i;
                }
            }
        }
        if (bestIndex == -1) {
            bestIndex = 0;
            slotRefCounts[bestIndex] = 0;
        }
        configureSlot(bestIndex, newSnap);
        return bestIndex;
    }

    private static void configureSlot(int index, ParamSnapshot snap) {
        int slotId = auxEffectSlots[index];
        int effectId = reverbEffects[index];
        if (snap.params != null && snap.params.length >= ReverbDefinition.PARAM_COUNT) {
            applyParamsToEffect(effectId, snap.params);
            EXTEfx.alAuxiliaryEffectSloti(slotId, EXTEfx.AL_EFFECTSLOT_EFFECT, effectId);
        }
        slotSnapshots[index] = snap;
        slotReleaseTime[index] = System.currentTimeMillis();
    }

    public static void ensureInitialized() {
        try {
            long currentCtx = ALC10.alcGetCurrentContext();
            if (currentCtx != contextPointer || !initialized) {
                if (initialized)
                    cleanup();
                contextPointer = currentCtx;
                init();
            }
        } catch (Throwable t) {
            initialized = false;
        }
    }

    private static void cleanup() {
        try {
            if (auxEffectSlots != null) {
                for (int id : auxEffectSlots)
                    if (id > 0)
                        try {
                            EXTEfx.alDeleteAuxiliaryEffectSlots(id);
                        } catch (Exception e) {
                        }
            }
            if (reverbEffects != null) {
                for (int id : reverbEffects)
                    if (id > 0)
                        try {
                            EXTEfx.alDeleteEffects(id);
                        } catch (Exception e) {
                        }
            }
            // [删除] directFilters 清理
            if (sharedFilter > 0)
                try {
                    EXTEfx.alDeleteFilters(sharedFilter);
                } catch (Exception e) {
                }
        } catch (Exception e) {
        }

        auxEffectSlots = null;
        reverbEffects = null;
        // [删除] directFilters
        slotSnapshots = null;
        slotRefCounts = null;
        slotReleaseTime = null;
        sourceSlotMap.clear();
        sharedFilter = 0;
        activeSlots = 0;
        initialized = false;
        supported = false;
    }

    private static void init() {
        try {
            long device = ALC10.alcGetContextsDevice(contextPointer);
            if (ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
                AL10.alGetError();
                if (setupEFX()) {
                    supported = true;
                    LOGGER.info("OpenAL EFX initialized. Slots: " + activeSlots);
                } else {
                    supported = false;
                    cleanup();
                }
            } else {
                supported = false;
            }
        } catch (Exception e) {
            supported = false;
        }
        initialized = true;
    }

    private static boolean setupEFX() {
        int[] tempAux = new int[targetSlots];
        int[] tempEffects = new int[targetSlots];
        // [删除] tempFilters 数组

        sharedFilter = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(sharedFilter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        EXTEfx.alFilterf(sharedFilter, EXTEfx.AL_LOWPASS_GAIN, 1.0f);

        int count = 0;
        for (int i = 0; i < targetSlots; i++) {
            int aux = EXTEfx.alGenAuxiliaryEffectSlots();
            if (AL10.alGetError() != AL10.AL_NO_ERROR)
                break;
            EXTEfx.alAuxiliaryEffectSloti(aux, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL11.AL_TRUE);

            int effect = EXTEfx.alGenEffects();
            if (AL10.alGetError() != AL10.AL_NO_ERROR)
                break;
            EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
            EXTEfx.alAuxiliaryEffectSloti(aux, EXTEfx.AL_EFFECTSLOT_EFFECT, effect);

            // [删除] 每个槽位的 Filter 生成逻辑 (因为不再需要 Direct Filter)

            tempAux[i] = aux;
            tempEffects[i] = effect;
            count++;
        }

        if (count == 0)
            return false;

        activeSlots = count;
        auxEffectSlots = new int[activeSlots];
        reverbEffects = new int[activeSlots];
        // [删除] directFilters 赋值
        System.arraycopy(tempAux, 0, auxEffectSlots, 0, activeSlots);
        System.arraycopy(tempEffects, 0, reverbEffects, 0, activeSlots);

        slotSnapshots = new ParamSnapshot[activeSlots];
        slotRefCounts = new int[activeSlots];
        slotReleaseTime = new long[activeSlots];

        return true;
    }

    public static int getUsedSlotCount() {
        if (!supported)
            return 0;
        int used = 0;
        for (int c : slotRefCounts)
            if (c > 0)
                used++;
        return used;
    }

    public static int getMaxSlots() {
        ensureInitialized();
        return supported ? activeSlots : 0;
    }

    private static void applyParamsToEffect(int effectId, float[] p) {
        // ... (保持 Reverb 参数应用逻辑完全不变)
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_DENSITY, p[ReverbDefinition.DENSITY]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_DIFFUSION, p[ReverbDefinition.DIFFUSION]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_GAIN, p[ReverbDefinition.GAIN]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_GAINHF, p[ReverbDefinition.GAIN_HF]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_GAINLF, p[ReverbDefinition.GAIN_LF]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_DECAY_TIME, p[ReverbDefinition.DECAY_TIME]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, p[ReverbDefinition.DECAY_HF_RATIO]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_DECAY_LFRATIO, p[ReverbDefinition.DECAY_LF_RATIO]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, p[ReverbDefinition.REFLECTIONS_GAIN]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY, p[ReverbDefinition.REFLECTIONS_DELAY]);
        EXTEfx.alEffectfv(effectId, EXTEfx.AL_EAXREVERB_REFLECTIONS_PAN,
                new float[] { p[ReverbDefinition.REFLECTIONS_PAN_X], p[ReverbDefinition.REFLECTIONS_PAN_Y],
                        p[ReverbDefinition.REFLECTIONS_PAN_Z] });
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, p[ReverbDefinition.LATE_REVERB_GAIN]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, p[ReverbDefinition.LATE_REVERB_DELAY]);
        EXTEfx.alEffectfv(effectId, EXTEfx.AL_EAXREVERB_LATE_REVERB_PAN,
                new float[] { p[ReverbDefinition.LATE_REVERB_PAN_X], p[ReverbDefinition.LATE_REVERB_PAN_Y],
                        p[ReverbDefinition.LATE_REVERB_PAN_Z] });
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_ECHO_TIME, p[ReverbDefinition.ECHO_TIME]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_ECHO_DEPTH, p[ReverbDefinition.ECHO_DEPTH]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_MODULATION_TIME, p[ReverbDefinition.MODULATION_TIME]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_MODULATION_DEPTH, p[ReverbDefinition.MODULATION_DEPTH]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF,
                p[ReverbDefinition.AIR_ABSORPTION_GAIN_HF]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_HFREFERENCE, p[ReverbDefinition.HF_REFERENCE]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_LFREFERENCE, p[ReverbDefinition.LF_REFERENCE]);
        EXTEfx.alEffectf(effectId, EXTEfx.AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, p[ReverbDefinition.ROOM_ROLLOFF_FACTOR]);
        EXTEfx.alEffecti(effectId, EXTEfx.AL_EAXREVERB_DECAY_HFLIMIT,
                p[ReverbDefinition.DECAY_HF_LIMIT] > 0.5f ? 1 : 0);
    }
}