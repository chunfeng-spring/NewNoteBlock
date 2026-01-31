package com.chunfeng.newnoteblock.audio.dsp;

import com.chunfeng.newnoteblock.audio.data.FilterDefinition;

public class ThreeBandEQ {
    private final BiquadFilter lowShelf = new BiquadFilter();
    private final BiquadFilter peaking = new BiquadFilter();
    private final BiquadFilter highShelf = new BiquadFilter();
    private final float sampleRate;

    public ThreeBandEQ(float sampleRate) {
        this.sampleRate = sampleRate;
        updateParameters(FilterDefinition.getDefault());
    }

    public void updateParameters(float[] params) {
        if (params == null || params.length != FilterDefinition.PARAM_COUNT) return;

        float lowFreq = params[FilterDefinition.LOW_FREQ];
        float highFreq = params[FilterDefinition.HIGH_FREQ];
        // 自动计算中频频率 (几何平均值，保证在 Log 坐标上居中)
        float midFreq = (float) Math.sqrt(lowFreq * highFreq);

        lowShelf.calculateCoefficients(BiquadFilter.Type.LOW_SHELF, sampleRate, lowFreq, params[FilterDefinition.LOW_Q], params[FilterDefinition.LOW_GAIN]);
        peaking.calculateCoefficients(BiquadFilter.Type.PEAKING_EQ, sampleRate, midFreq, params[FilterDefinition.MID_Q], params[FilterDefinition.MID_GAIN]);
        highShelf.calculateCoefficients(BiquadFilter.Type.HIGH_SHELF, sampleRate, highFreq, params[FilterDefinition.HIGH_Q], params[FilterDefinition.HIGH_GAIN]);
    }

    public void processBlock(float[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            float sample = buffer[i];
            sample = lowShelf.process(sample);
            sample = peaking.process(sample);
            sample = highShelf.process(sample);
            buffer[i] = sample;
        }
    }

    // 重置滤波器历史状态 (防止爆音)
    public void reset() {
        lowShelf.reset();
        peaking.reset();
        highShelf.reset();
    }
}