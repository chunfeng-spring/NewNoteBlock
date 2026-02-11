package com.chunfeng.newnoteblock.audio.dsp;

/**
 * 标准 Biquad 滤波器实现 (基于 RBJ Audio EQ Cookbook)
 * 公式: y[n] = b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]
 */
public class BiquadFilter {
    // 滤波器类型
    public enum Type {
        LOW_SHELF,
        HIGH_SHELF,
        PEAKING_EQ
    }

    // 系数
    private float a0, a1, a2, b0, b1, b2;
    // 历史状态 (x[n-1], x[n-2], y[n-1], y[n-2])
    private float x1, x2, y1, y2;

    public BiquadFilter() {
        reset();
    }

    public void reset() {
        x1 = x2 = y1 = y2 = 0;
        // 默认直通 (b0=1, 其余0)
        b0 = 1;
        a0 = 1;
        b1 = b2 = a1 = a2 = 0;
    }

    /**
     * 计算滤波器系数
     * 
     * @param type       滤波器类型
     * @param sampleRate 采样率 (Hz)
     * @param freq       中心频率/截止频率 (Hz)
     * @param Q          品质因子/斜率 (通常 0.1 ~ 10.0, Shelf滤波器常用 0.707)
     * @param gainDB     增益 (dB)
     */
    public void calculateCoefficients(Type type, float sampleRate, float freq, float Q, float gainDB) {
        float w0 = (float) (2 * Math.PI * freq / sampleRate);
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float alpha = sinW0 / (2 * Q);
        // Shelf 用 dB/20, Peaking 用 dB/40 (RBJ Audio EQ Cookbook)
        float A = (float) Math.pow(10, gainDB / (type == Type.PEAKING_EQ ? 40.0 : 20.0));

        switch (type) {
            case LOW_SHELF:
                float sqrtA = (float) Math.sqrt(A);
                b0 = A * ((A + 1) - (A - 1) * cosW0 + 2 * sqrtA * alpha);
                b1 = 2 * A * ((A - 1) - (A + 1) * cosW0);
                b2 = A * ((A + 1) - (A - 1) * cosW0 - 2 * sqrtA * alpha);
                a0 = (A + 1) + (A - 1) * cosW0 + 2 * sqrtA * alpha;
                a1 = -2 * ((A - 1) + (A + 1) * cosW0);
                a2 = (A + 1) + (A - 1) * cosW0 - 2 * sqrtA * alpha;
                break;

            case HIGH_SHELF:
                sqrtA = (float) Math.sqrt(A);
                b0 = A * ((A + 1) + (A - 1) * cosW0 + 2 * sqrtA * alpha);
                b1 = -2 * A * ((A - 1) + (A + 1) * cosW0);
                b2 = A * ((A + 1) + (A - 1) * cosW0 - 2 * sqrtA * alpha);
                a0 = (A + 1) - (A - 1) * cosW0 + 2 * sqrtA * alpha;
                a1 = 2 * ((A - 1) - (A + 1) * cosW0);
                a2 = (A + 1) - (A - 1) * cosW0 - 2 * sqrtA * alpha;
                break;

            case PEAKING_EQ:
                b0 = 1 + alpha * A;
                b1 = -2 * cosW0;
                b2 = 1 - alpha * A;
                a0 = 1 + alpha / A;
                a1 = -2 * cosW0;
                a2 = 1 - alpha / A;
                break;
        }

        // 归一化系数 (让 a0 = 1)
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;
    }

    public float process(float input) {
        // 差分方程
        float output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

        // Denormal 保护：防止极小浮点数导致 CPU 性能骤降
        if (Math.abs(output) < 1e-18f)
            output = 0f;

        // 更新历史状态 (移位)
        x2 = x1;
        x1 = input;
        y2 = y1;
        y1 = output;

        return output;
    }
}