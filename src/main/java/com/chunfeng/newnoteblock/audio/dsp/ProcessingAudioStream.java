package com.chunfeng.newnoteblock.audio.dsp;

import net.minecraft.client.sound.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ProcessingAudioStream implements AudioStream {
    private final AudioStream delegate;
    private final ThreeBandEQ eq;
    private final AudioFormat format;

    // 内部缓冲区，用于 byte <-> float 转换
    private float[] floatBuffer;

    public ProcessingAudioStream(AudioStream delegate, float[] eqParams) {
        this.delegate = delegate;
        this.format = delegate.getFormat();

        // 初始化 EQ
        this.eq = new ThreeBandEQ(format.getSampleRate());
        this.eq.updateParameters(eqParams);
    }

    @Override
    public AudioFormat getFormat() {
        return delegate.getFormat();
    }

    @Override
    public ByteBuffer getBuffer(int size) throws IOException {
        // 1. 读取原始 PCM 数据
        ByteBuffer rawBuffer = delegate.getBuffer(size);

        if (rawBuffer == null || rawBuffer.limit() == 0) return rawBuffer;

        // 仅支持 16-bit 音频 (Minecraft 标准)
        if (format.getSampleSizeInBits() != 16) return rawBuffer;

        int sampleCount = rawBuffer.limit() / 2;

        // 准备 float 缓冲区
        if (floatBuffer == null || floatBuffer.length < sampleCount) {
            floatBuffer = new float[sampleCount];
        }

        // 2. Byte -> Float
        rawBuffer.order(ByteOrder.LITTLE_ENDIAN);
        rawBuffer.rewind(); // 此时 position=0

        for (int i = 0; i < sampleCount; i++) {
            short pcm = rawBuffer.getShort();
            floatBuffer[i] = pcm / 32768.0f; // 归一化
        }

        // 3. 应用 DSP
        eq.processBlock(floatBuffer);

        // 4. Float -> Byte
        rawBuffer.clear(); // 重置 position=0, limit=capacity

        for (int i = 0; i < sampleCount; i++) {
            float sample = floatBuffer[i];

            // 硬限幅 (Hard Clipping) 防止爆音
            if (sample > 1.0f) sample = 1.0f;
            if (sample < -1.0f) sample = -1.0f;

            short pcm = (short) (sample * 32767.0f);
            rawBuffer.putShort(pcm);
        }

        // 准备读取
        rawBuffer.flip();
        return rawBuffer;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}