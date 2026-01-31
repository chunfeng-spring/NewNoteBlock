package com.chunfeng.newnoteblock.audio.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定义 EAX 混响效果器 (AL_EFFECT_EAXREVERB) 的参数索引和预设值
 * 参数顺序严格对应 OpenAL EAX 规范
 */
public class ReverbDefinition {
    // ==========================================
    // 参数索引定义 (Flat Array Index)
    // ==========================================
    public static final int DENSITY = 0;
    public static final int DIFFUSION = 1;
    public static final int GAIN = 2;
    public static final int GAIN_HF = 3;
    public static final int GAIN_LF = 4; // New
    public static final int DECAY_TIME = 5;
    public static final int DECAY_HF_RATIO = 6;
    public static final int DECAY_LF_RATIO = 7; // New
    public static final int REFLECTIONS_GAIN = 8;
    public static final int REFLECTIONS_DELAY = 9;
    public static final int REFLECTIONS_PAN_X = 10; // New (Vector3f split)
    public static final int REFLECTIONS_PAN_Y = 11;
    public static final int REFLECTIONS_PAN_Z = 12;
    public static final int LATE_REVERB_GAIN = 13;
    public static final int LATE_REVERB_DELAY = 14;
    public static final int LATE_REVERB_PAN_X = 15; // New (Vector3f split)
    public static final int LATE_REVERB_PAN_Y = 16;
    public static final int LATE_REVERB_PAN_Z = 17;
    public static final int ECHO_TIME = 18; // New
    public static final int ECHO_DEPTH = 19; // New
    public static final int MODULATION_TIME = 20; // New
    public static final int MODULATION_DEPTH = 21; // New
    public static final int AIR_ABSORPTION_GAIN_HF = 22;
    public static final int HF_REFERENCE = 23; // New
    public static final int LF_REFERENCE = 24; // New
    public static final int ROOM_ROLLOFF_FACTOR = 25;
    public static final int DECAY_HF_LIMIT = 26; // New (Boolean as float 0.0/1.0)

    // 总参数数量 (27个 float)
    public static final int PARAM_COUNT = 27;

    public static final String[] PRESET_NAMES;
    public static final Map<String, float[]> PRESETS = new LinkedHashMap<>();

    static {
        // --- 预设库 (EAX 2.0 Standard Presets) ---
        // 为了节省篇幅，这里初始化一个默认的全 0 数组生成器，然后填充关键值
        // 实际开发中建议做成配置文件，这里硬编码常用预设

        // Default (Generic)
        addPreset("Generic (默认)", 1.0f, 1.0f, 0.32f, 0.89f, 0.0f, 1.49f, 0.83f, 1.0f, 0.05f, 0.007f, 1.26f, 0.011f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Padded Cell
        addPreset("Padded Cell (软垫房)", 0.14f, 1.0f, 0.17f, 0.99f, 1.0f, 0.17f, 0.10f, 1.0f, 0.17f, 0.001f, 1.26f, 0.001f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Room
        addPreset("Room (房间)", 0.19f, 1.0f, 0.41f, 0.83f, 1.0f, 0.40f, 0.83f, 1.0f, 0.4f, 0.002f, 1.26f, 0.003f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Bathroom
        addPreset("Bathroom (浴室)", 0.41f, 1.0f, 0.6f, 0.60f, 1.0f, 1.53f, 0.83f, 1.0f, 0.6f, 0.007f, 1.26f, 0.011f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Living Room
        addPreset("Living Room (客厅)", 0.21f, 1.0f, 0.20f, 0.60f, 1.0f, 0.50f, 0.83f, 1.0f, 0.20f, 0.003f, 1.26f, 0.004f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Stone Room
        addPreset("Stone Room (石室)", 0.5f, 1.0f, 0.5f, 0.67f, 1.0f, 2.31f, 0.64f, 1.0f, 0.5f, 0.012f, 1.26f, 0.017f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Auditorium
        addPreset("Auditorium (礼堂)", 1.0f, 1.0f, 0.4f, 0.89f, 1.0f, 4.32f, 0.83f, 1.0f, 0.4f, 0.020f, 1.26f, 0.030f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Concert Hall
        addPreset("Concert Hall (音乐厅)", 1.0f, 1.0f, 0.5f, 0.89f, 1.0f, 3.92f, 0.83f, 1.0f, 0.5f, 0.020f, 1.26f, 0.029f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Cave
        addPreset("Cave (洞穴)", 1.0f, 1.0f, 0.6f, 1.00f, 1.0f, 2.91f, 1.30f, 1.0f, 0.6f, 0.015f, 1.26f, 0.022f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Arena
        addPreset("Arena (竞技场)", 1.0f, 1.0f, 0.4f, 0.50f, 1.0f, 7.24f, 0.33f, 1.0f, 0.4f, 0.020f, 1.26f, 0.030f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Hangar
        addPreset("Hangar (机库)", 1.0f, 1.0f, 0.5f, 0.50f, 1.0f, 10.05f, 0.23f, 1.0f, 0.5f, 0.020f, 1.26f, 0.030f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Carpeted Hallway
        addPreset("Carpeted Hallway (地毯走廊)", 0.15f, 1.0f, 0.16f, 0.05f, 1.0f, 1.82f, 0.1f, 1.0f, 0.16f, 0.002f, 1.26f, 0.030f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Hallway
        addPreset("Hallway (走廊)", 0.24f, 1.0f, 0.3f, 0.67f, 1.0f, 1.66f, 0.64f, 1.0f, 0.3f, 0.006f, 1.26f, 0.021f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Stone Corridor
        addPreset("Stone Corridor (石制走廊)", 0.44f, 1.0f, 0.3f, 0.77f, 1.0f, 2.37f, 0.53f, 1.0f, 0.3f, 0.013f, 1.26f, 0.014f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Alley
        addPreset("Alley (小巷)", 0.3f, 0.3f, 0.4f, 0.73f, 1.0f, 1.62f, 0.83f, 1.0f, 0.4f, 0.012f, 1.26f, 0.013f, 0.25f, 0.1f, 0.25f, 0.1f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Forest
        addPreset("Forest (森林)", 0.3f, 0.3f, 0.2f, 0.06f, 1.0f, 1.51f, 0.12f, 1.0f, 0.2f, 0.151f, 1.26f, 0.091f, 0.25f, 0.1f, 0.25f, 0.1f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // City
        addPreset("City (城市)", 0.5f, 0.3f, 0.2f, 0.15f, 1.0f, 1.57f, 0.25f, 1.0f, 0.2f, 0.007f, 1.26f, 0.011f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Mountains
        addPreset("Mountains (山脉)", 0.27f, 0.3f, 0.1f, 0.05f, 1.0f, 2.50f, 0.22f, 1.0f, 0.1f, 0.0f, 1.26f, 0.1f, 0.25f, 0.2f, 0.25f, 0.2f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Quarry
        addPreset("Quarry (采石场)", 1.0f, 1.0f, 0.5f, 0.50f, 1.0f, 1.49f, 0.54f, 1.0f, 0.5f, 0.057f, 1.26f, 0.061f, 0.25f, 0.12f, 0.25f, 0.13f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Sewer Pipe
        addPreset("Sewer Pipe (下水道)", 0.8f, 0.8f, 0.6f, 0.50f, 1.0f, 2.91f, 0.29f, 1.0f, 0.6f, 0.014f, 1.26f, 0.021f, 0.25f, 0.0f, 0.25f, 0.0f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        // Underwater
        addPreset("Underwater (水下)", 1.0f, 1.0f, 0.6f, 0.20f, 1.0f, 1.49f, 0.10f, 1.0f, 0.6f, 0.007f, 1.26f, 0.011f, 0.25f, 0.0f, 1.18f, 0.35f, 0.994f, 5000.0f, 250.0f, 0.0f, 1.0f);

        PRESET_NAMES = PRESETS.keySet().toArray(new String[0]);
    }

    /**
     * 辅助方法：快速添加预设 (参数顺序对应上面的索引)
     * 这里简化了 Pan (设为0)
     */
    private static void addPreset(String name,
                                  float density, float diffusion, float gain, float gainHF, float gainLF,
                                  float decayTime, float decayHFRatio, float decayLFRatio,
                                  float refGain, float refDelay,
                                  float lateGain, float lateDelay,
                                  float echoTime, float echoDepth,
                                  float modTime, float modDepth,
                                  float airAbsorb, float hfRef, float lfRef, float rolloff, float limit) {
        float[] p = new float[PARAM_COUNT];
        p[DENSITY] = density;
        p[DIFFUSION] = diffusion;
        p[GAIN] = gain;
        p[GAIN_HF] = gainHF;
        p[GAIN_LF] = gainLF;
        p[DECAY_TIME] = decayTime;
        p[DECAY_HF_RATIO] = decayHFRatio;
        p[DECAY_LF_RATIO] = decayLFRatio;
        p[REFLECTIONS_GAIN] = refGain;
        p[REFLECTIONS_DELAY] = refDelay;
        // Pan 默认为 0
        p[REFLECTIONS_PAN_X] = 0f; p[REFLECTIONS_PAN_Y] = 0f; p[REFLECTIONS_PAN_Z] = 0f;
        p[LATE_REVERB_GAIN] = lateGain;
        p[LATE_REVERB_DELAY] = lateDelay;
        // Pan 默认为 0
        p[LATE_REVERB_PAN_X] = 0f; p[LATE_REVERB_PAN_Y] = 0f; p[LATE_REVERB_PAN_Z] = 0f;
        p[ECHO_TIME] = echoTime;
        p[ECHO_DEPTH] = echoDepth;
        p[MODULATION_TIME] = modTime;
        p[MODULATION_DEPTH] = modDepth;
        p[AIR_ABSORPTION_GAIN_HF] = airAbsorb;
        p[HF_REFERENCE] = hfRef;
        p[LF_REFERENCE] = lfRef;
        p[ROOM_ROLLOFF_FACTOR] = rolloff;
        p[DECAY_HF_LIMIT] = limit;

        PRESETS.put(name, p);
    }

    public static float[] getDefault() {
        return PRESETS.get("Generic (默认)").clone();
    }
}