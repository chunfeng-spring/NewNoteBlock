package com.chunfeng.newnoteblock.audio.manager;

import com.chunfeng.newnoteblock.client.ui.screen.NewNoteBlockScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jtransforms.fft.DoubleFFT_1D;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioAnalyzer {

    private static final float STANDARD_SAMPLE_RATE = 44100.0f;
    private static final int FFT_SIZE = 4096;
    private static final int OUTPUT_BINS = 256;
    private static final int TARGET_FPS = 60;
    private static final long FRAME_DELAY_MS = 1000 / TARGET_FPS;

    private static Thread currentAnalysisThread;
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    private static class RawAudio {
        float[] data;
        int sampleRate;

        public RawAudio(float[] data, int sampleRate) {
            this.data = data;
            this.sampleRate = sampleRate;
        }
    }

    public static void updateSpectrumAsync(String instrument, int note, int pitchRange) {
        stopCurrentAnalysis();

        currentAnalysisThread = new Thread(() -> {
            isRunning.set(true);
            runDynamicAnalysis(instrument, note);
        }, "Spectrum-Dynamic-Thread");

        currentAnalysisThread.setDaemon(true);
        currentAnalysisThread.start();
    }

    public static void stop() {
        stopCurrentAnalysis();
    }

    private static void stopCurrentAnalysis() {
        isRunning.set(false);
        if (currentAnalysisThread != null) {
            try {
                currentAnalysisThread.join(50);
            } catch (InterruptedException ignored) {
            }
            currentAnalysisThread = null;
        }
        Arrays.fill(NewNoteBlockScreen.cachedSpectrumData, 0.0f);
    }

    private static void runDynamicAnalysis(String instrument, int note) {
        try {
            SamplerManager.SampleResult result = SamplerManager.getBestSample(instrument, note);

            Identifier soundId;
            float notePitchMult;

            if (result != null) {
                soundId = result.soundId;
                notePitchMult = result.pitchMultiplier;
            } else {
                soundId = new Identifier("minecraft", "block.note_block." + instrument);
                notePitchMult = (float) Math.pow(2.0, (note - 12) / 12.0);
            }

            RawAudio rawAudio = loadAndDecodeOgg(soundId);
            if (rawAudio == null || rawAudio.data.length == 0)
                return;

            float sourceRateCorrection = rawAudio.sampleRate / STANDARD_SAMPLE_RATE;
            float totalSpeed = sourceRateCorrection * notePitchMult;

            float[] audioData = resample(rawAudio.data, totalSpeed);

            DoubleFFT_1D fft = new DoubleFFT_1D(FFT_SIZE);
            double[] fftBuffer = new double[FFT_SIZE * 2];
            float[] windowData = new float[FFT_SIZE];

            int stepSize = (int) (STANDARD_SAMPLE_RATE / TARGET_FPS);
            int cursor = 0;

            while (isRunning.get() && cursor < audioData.length) {
                long frameStart = System.currentTimeMillis();

                int readLength = Math.min(FFT_SIZE, audioData.length - cursor);
                System.arraycopy(audioData, cursor, windowData, 0, readLength);
                if (readLength < FFT_SIZE)
                    Arrays.fill(windowData, readLength, FFT_SIZE, 0.0f);

                float[] magnitudes = computeFFT(windowData, fft, fftBuffer);

                float[] logSpectrum = mapToLogarithmicScale(magnitudes, STANDARD_SAMPLE_RATE);

                synchronized (NewNoteBlockScreen.cachedSpectrumData) {
                    float[] cached = NewNoteBlockScreen.cachedSpectrumData;
                    for (int i = 0; i < Math.min(logSpectrum.length, cached.length); i++) {
                        if (logSpectrum[i] > cached[i]) {
                            cached[i] = logSpectrum[i];
                        } else {
                            cached[i] = cached[i] * 0.85f;
                        }
                    }
                }

                cursor += stepSize;

                long workTime = System.currentTimeMillis() - frameStart;
                long sleepTime = FRAME_DELAY_MS - workTime;
                if (sleepTime > 0)
                    Thread.sleep(sleepTime);
            }

            performFadeOut();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (Thread.currentThread() == currentAnalysisThread) {
                isRunning.set(false);
            }
        }
    }

    private static RawAudio loadAndDecodeOgg(Identifier soundId) {
        MinecraftClient client = MinecraftClient.getInstance();
        WeightedSoundSet soundSet = client.getSoundManager().get(soundId);
        if (soundSet == null)
            return null;
        Sound sound = soundSet.getSound(Random.create());
        if (sound == null)
            return null;
        Identifier resourceLocation = sound.getLocation();

        ByteBuffer vorbisBuffer = null;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Resource resource = client.getResourceManager().getResource(resourceLocation).orElse(null);
            if (resource == null)
                return null;

            try (InputStream is = resource.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                vorbisBuffer = MemoryUtil.memAlloc(bytes.length);
                vorbisBuffer.put(bytes);
                vorbisBuffer.flip();
            }

            IntBuffer error = stack.mallocInt(1);
            long handle = STBVorbis.stb_vorbis_open_memory(vorbisBuffer, error, null);
            if (handle == 0) {
                MemoryUtil.memFree(vorbisBuffer);
                return null;
            }

            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(handle, info);
            int channels = info.channels();
            int sampleRate = info.sample_rate();

            int samplesPerChannel = STBVorbis.stb_vorbis_stream_length_in_samples(handle);
            ShortBuffer pcmShort = MemoryUtil.memAllocShort(samplesPerChannel * channels);
            STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, pcmShort);
            STBVorbis.stb_vorbis_close(handle);

            float[] monoData = new float[samplesPerChannel];

            if (channels == 1) {
                for (int i = 0; i < samplesPerChannel; i++) {
                    monoData[i] = pcmShort.get(i) / 32768.0f;
                }
            } else if (channels == 2) {
                for (int i = 0; i < samplesPerChannel; i++) {
                    short left = pcmShort.get(i * 2);
                    short right = pcmShort.get(i * 2 + 1);
                    monoData[i] = ((left + right) * 0.5f) / 32768.0f;
                }
            } else {
                for (int i = 0; i < samplesPerChannel; i++) {
                    float sum = 0;
                    for (int c = 0; c < channels; c++) {
                        sum += pcmShort.get(i * channels + c);
                    }
                    monoData[i] = (sum / channels) / 32768.0f;
                }
            }

            MemoryUtil.memFree(pcmShort);
            MemoryUtil.memFree(vorbisBuffer);

            return new RawAudio(monoData, sampleRate);

        } catch (IOException e) {
            if (vorbisBuffer != null)
                MemoryUtil.memFree(vorbisBuffer);
            e.printStackTrace();
            return null;
        }
    }

    private static float[] computeFFT(float[] windowData, DoubleFFT_1D fft, double[] fftBuffer) {
        Arrays.fill(fftBuffer, 0.0);

        // 窗函数总权重 (用于归一化补偿)
        // 汉宁窗的相干增益(Coherent Gain)是 0.5，所以幅度会变为原来的一半
        // 我们需要在结果中乘 2 来补偿

        for (int i = 0; i < FFT_SIZE; i++) {
            double window = 0.5 * (1 - Math.cos(2 * Math.PI * i / (FFT_SIZE - 1)));
            fftBuffer[2 * i] = windowData[i] * window;
            fftBuffer[2 * i + 1] = 0;
        }

        fft.complexForward(fftBuffer);

        float[] magnitude = new float[FFT_SIZE / 2];

        // [核心修复] 归一化系数
        // 对于 JTransforms: 幅度需要除以 (FFT_SIZE / 2) 才能得到真实的物理振幅
        // 另外因为加了汉宁窗(损失50%幅度)，需要乘以 2 进行补偿
        // 最终系数: 2.0 / (FFT_SIZE / 2) = 4.0 / FFT_SIZE
        float normalizationFactor = 4.0f / FFT_SIZE;

        for (int i = 0; i < magnitude.length; i++) {
            double re = fftBuffer[2 * i];
            double im = fftBuffer[2 * i + 1];

            // 原始模长
            float rawMag = (float) Math.sqrt(re * re + im * im);

            // 归一化后的模长 (现在这个值就在 0.0 ~ 1.0 之间了)
            magnitude[i] = rawMag * normalizationFactor;
        }
        return magnitude;
    }

    private static float[] resample(float[] input, float speed) {
        if (Math.abs(speed - 1.0f) < 0.001f)
            return input;

        int newLength = (int) (input.length / speed);
        if (newLength < FFT_SIZE) {
            float[] padded = new float[FFT_SIZE];
            System.arraycopy(input, 0, padded, 0, Math.min(input.length, FFT_SIZE));
            return padded;
        }

        float[] output = new float[newLength];
        for (int i = 0; i < newLength; i++) {
            float srcPos = i * speed;
            int index = (int) srcPos;
            float frac = srcPos - index;

            if (index >= input.length - 1) {
                output[i] = input[input.length - 1];
            } else {
                output[i] = input[index] * (1.0f - frac) + input[index + 1] * frac;
            }
        }
        return output;
    }

    private static float[] mapToLogarithmicScale(float[] fftData, float sampleRate) {
        float[] output = new float[OUTPUT_BINS];

        double minLog = Math.log10(20);
        double maxLog = Math.log10(20000);
        double rangeLog = maxLog - minLog;
        double binSize = sampleRate / FFT_SIZE;

        for (int i = 0; i < OUTPUT_BINS; i++) {
            double normalizedX = (double) i / (OUTPUT_BINS - 1);
            double targetFreq = Math.pow(10, minLog + normalizedX * rangeLog);
            int fftIndex = (int) (targetFreq / binSize);

            float val;
            if (fftIndex < 0)
                val = fftData[0];
            else if (fftIndex >= fftData.length - 1)
                val = fftData[fftData.length - 1];
            else {
                float frac = (float) ((targetFreq / binSize) - fftIndex);
                val = fftData[fftIndex] * (1 - frac) + fftData[fftIndex + 1] * frac;
            }

            // [标准 dB 计算]
            // val 现在最大是 1.0 (0dB)
            // 加上 1e-9 防止 log(0)
            float db = (float) (20 * Math.log10(val + 1e-9));

            // [标准映射]
            // 显示范围: -60dB (底部) 到 0dB (顶部)
            // 超过 0dB 的会被 Clamp 到 1.0，低于 -60dB 的会被 Clamp 到 0.0
            float normalizedVal = (db + 60.0f) / 60.0f;

            output[i] = Math.max(0.0f, Math.min(1.0f, normalizedVal));
        }
        return output;
    }

    private static void performFadeOut() {
        float[] guiData = NewNoteBlockScreen.cachedSpectrumData;
        for (int f = 0; f < 20; f++) {
            boolean active = false;
            for (int i = 0; i < guiData.length; i++) {
                guiData[i] *= 0.8f;
                if (guiData[i] > 0.01f)
                    active = true;
            }
            if (!active)
                break;
            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {
            }
        }
        Arrays.fill(guiData, 0.0f);
    }
}