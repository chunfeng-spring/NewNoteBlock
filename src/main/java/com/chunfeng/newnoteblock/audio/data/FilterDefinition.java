package com.chunfeng.newnoteblock.audio.data;

public class FilterDefinition {
    // 默认值
    public static final float DEFAULT_LOW_FREQ = 200.0f;
    public static final float DEFAULT_HIGH_FREQ = 4000.0f;
    public static final float DEFAULT_LOW_GAIN = 0.0f; // dB
    public static final float DEFAULT_MID_GAIN = 0.0f; // dB
    public static final float DEFAULT_HIGH_GAIN = 0.0f; // dB
    public static final float DEFAULT_LOW_Q = 0.707f;
    public static final float DEFAULT_MID_Q = 1.0f;
    public static final float DEFAULT_HIGH_Q = 0.707f;

    // 参数索引 (用于数组/网络传输)
    public static final int LOW_FREQ = 0;
    public static final int HIGH_FREQ = 1;
    public static final int LOW_GAIN = 2;
    public static final int MID_GAIN = 3;
    public static final int HIGH_GAIN = 4;
    public static final int LOW_Q = 5;
    public static final int MID_Q = 6;
    public static final int HIGH_Q = 7;

    public static final int PARAM_COUNT = 8;

    public static float[] getDefault() {
        return new float[] {
                DEFAULT_LOW_FREQ, DEFAULT_HIGH_FREQ,
                DEFAULT_LOW_GAIN, DEFAULT_MID_GAIN, DEFAULT_HIGH_GAIN,
                DEFAULT_LOW_Q, DEFAULT_MID_Q, DEFAULT_HIGH_Q
        };
    }

    public static boolean isNeutral(float[] params) {
        // 检查低、中、高频的增益是否都接近 0
        // 浮点数比较建议使用一个微小的 epsilon
        float epsilon = 0.001f;
        return Math.abs(params[LOW_GAIN]) < epsilon &&
                Math.abs(params[MID_GAIN]) < epsilon &&
                Math.abs(params[HIGH_GAIN]) < epsilon;
    }
}