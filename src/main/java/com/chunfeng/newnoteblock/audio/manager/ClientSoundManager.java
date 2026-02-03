package com.chunfeng.newnoteblock.audio.manager;

import com.chunfeng.newnoteblock.audio.engine.NoteBlockAudioEngine;
import com.chunfeng.newnoteblock.audio.data.ReverbDefinition;
import com.chunfeng.newnoteblock.audio.data.FilterDefinition;
import com.chunfeng.newnoteblock.audio.manager.SamplerManager.SampleResult;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSoundManager {
    private static final Map<UUID, ModSoundInstance> activeSounds = new ConcurrentHashMap<>();
    private static final Map<UUID, ClientSoundFader> clientFaders = new ConcurrentHashMap<>();
    private static boolean tickListenerRegistered = false;

    /**
     * 初始化客户端tick监听器，用于处理预览声音的淡入淡出
     */
    public static void ensureTickListenerRegistered() {
        if (!tickListenerRegistered) {
            tickListenerRegistered = true;
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (clientFaders.isEmpty())
                    return;
                Iterator<Map.Entry<UUID, ClientSoundFader>> iterator = clientFaders.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, ClientSoundFader> entry = iterator.next();
                    ClientSoundFader fader = entry.getValue();
                    boolean finished = fader.tick();
                    if (finished) {
                        stopSound(entry.getKey());
                        iterator.remove();
                    }
                }
            });
        }
    }

    // 接收预计算的 path 数据 (仅用于传递给可能需要的逻辑，虽然现在位置由服务器驱动)
    // 但实际上，客户端不再需要计算路径，只需要在 startSound 时设定初始位置即可。
    // 为了保持接口兼容性，我们保留参数，但在 SoundInstance 中不再使用 path 进行计算。
    public static void startSound(UUID id, BlockPos pos, String instrumentId, int note,
            float initVolume, float initPitchMult,
            int pitchRange,
            float reverbSend, float[] reverbParams,
            float[] eqParams,
            int startTick, int endTick, boolean motionMode, List<Vec3d> motionPath) {

        MinecraftClient client = MinecraftClient.getInstance();
        NoteBlockAudioEngine.ensureInitialized();

        SoundEvent event;
        float finalBasePitch;

        String cleanInstName = instrumentId.contains(":") ? instrumentId.split(":")[1] : instrumentId;
        SampleResult sampleResult = SamplerManager.getBestSample(cleanInstName, note);

        if (sampleResult != null) {
            event = SoundEvent.of(sampleResult.soundId);
            finalBasePitch = sampleResult.pitchMultiplier;
        } else {
            event = getSoundEventFromId(instrumentId);
            if (event == null)
                return;
            finalBasePitch = (float) Math.pow(2.0, (note - 66) / 12.0);
        }

        ModSoundInstance instance = new ModSoundInstance(
                event, pos, finalBasePitch, initVolume, initPitchMult,
                reverbSend, reverbParams, eqParams);

        // 如果有初始位置(虽然update会马上来，但设置一次更稳妥)
        if (motionPath != null && !motionPath.isEmpty() && startTick == 0) {
            Vec3d startPos = motionPath.get(0);
            if (motionMode) {
                instance.setPosition(pos.getX() + 0.5 + startPos.x, pos.getY() + 0.5 + startPos.y,
                        pos.getZ() + 0.5 + startPos.z);
            } else {
                instance.setPosition(startPos.x, startPos.y, startPos.z);
            }
        }

        activeSounds.put(id, instance);

        client.execute(() -> {
            client.getSoundManager().play(instance);
        });
    }

    public static void updateSoundState(UUID id, float volume, float pitchMultiplier, double x, double y, double z) {
        ModSoundInstance instance = activeSounds.get(id);
        if (instance != null) {
            instance.updateState(volume, pitchMultiplier);
            instance.setPosition(x, y, z);
        }
    }

    // 保留旧签名以防兼容性问题，转发调用
    public static void updateSoundState(UUID id, float volume, float pitchMultiplier) {
        ModSoundInstance instance = activeSounds.get(id);
        if (instance != null) {
            instance.updateState(volume, pitchMultiplier);
        }
    }

    public static void stopSound(UUID id) {
        ModSoundInstance instance = activeSounds.remove(id);
        if (instance != null)
            instance.finish();
        clientFaders.remove(id);
    }

    /**
     * 停止所有客户端预览声音的淡入淡出器
     */
    public static void stopAllClientFaders() {
        for (UUID id : new ArrayList<>(clientFaders.keySet())) {
            stopSound(id);
        }
        clientFaders.clear();
    }

    /**
     * 带有音量包络和音高包络的声音播放（用于客户端预览）
     * 
     * @param id           声音唯一标识
     * @param pos          声音位置
     * @param instrumentId 乐器ID
     * @param note         音符
     * @param baseVolume   主音量 (0.0 - 1.0)
     * @param volumeCurve  音量曲线 (每个值为0-100的百分比)
     * @param pitchCurve   音高曲线 (每个值为以分音为单位的偏移量)
     * @param pitchRange   音高范围
     * @param reverbSend   混响发送量
     * @param reverbParams 混响参数
     * @param eqParams     EQ参数
     * @param startTick    移动开始tick
     * @param endTick      移动结束tick
     * @param motionMode   移动模式
     * @param motionPath   移动路径
     */
    public static void startSoundWithEnvelope(UUID id, BlockPos pos, String instrumentId, int note,
            float baseVolume, List<Integer> volumeCurve, List<Integer> pitchCurve,
            int pitchRange,
            float reverbSend, float[] reverbParams,
            float[] eqParams,
            int startTick, int endTick, boolean motionMode, List<Vec3d> motionPath) {

        ensureTickListenerRegistered();

        MinecraftClient client = MinecraftClient.getInstance();
        NoteBlockAudioEngine.ensureInitialized();

        SoundEvent event;
        float finalBasePitch;

        String cleanInstName = instrumentId.contains(":") ? instrumentId.split(":")[1] : instrumentId;
        SampleResult sampleResult = SamplerManager.getBestSample(cleanInstName, note);

        if (sampleResult != null) {
            event = SoundEvent.of(sampleResult.soundId);
            finalBasePitch = sampleResult.pitchMultiplier;
        } else {
            event = getSoundEventFromId(instrumentId);
            if (event == null)
                return;
            finalBasePitch = (float) Math.pow(2.0, (note - 66) / 12.0);
        }

        // 计算初始音量和音高
        float initVol = baseVolume;
        float initPitchMult = 1.0f;

        boolean hasEnvelope = (volumeCurve != null && !volumeCurve.isEmpty());

        if (hasEnvelope) {
            initVol = (volumeCurve.get(0) / 100.0f) * baseVolume;

            if (pitchCurve != null && !pitchCurve.isEmpty()) {
                int pVal = pitchCurve.get(0);
                double semitones = pVal / 100.0;
                initPitchMult = (float) Math.pow(2.0, semitones / 12.0);
            }
        }

        ModSoundInstance instance = new ModSoundInstance(
                event, pos, finalBasePitch, initVol, initPitchMult,
                reverbSend, reverbParams, eqParams);

        // 如果有初始位置
        if (motionPath != null && !motionPath.isEmpty() && startTick == 0) {
            Vec3d startPos = motionPath.get(0);
            if (motionMode) {
                instance.setPosition(pos.getX() + 0.5 + startPos.x, pos.getY() + 0.5 + startPos.y,
                        pos.getZ() + 0.5 + startPos.z);
            } else {
                instance.setPosition(startPos.x, startPos.y, startPos.z);
            }
        }

        activeSounds.put(id, instance);

        // 如果有音量包络，创建客户端淡入淡出器
        if (hasEnvelope) {
            ClientSoundFader fader = new ClientSoundFader(
                    id, pos, baseVolume, volumeCurve, pitchCurve,
                    pitchRange, motionMode, motionPath, startTick, endTick);
            clientFaders.put(id, fader);
        }

        client.execute(() -> {
            client.getSoundManager().play(instance);
        });
    }

    private static SoundEvent getSoundEventFromId(String id) {
        Identifier identifier = id.contains(":") ? new Identifier(id)
                : new Identifier("minecraft", "block.note_block." + id);
        SoundEvent regEvent = Registries.SOUND_EVENT.get(identifier);
        return regEvent != null ? regEvent : SoundEvent.of(identifier);
    }

    public static class ModSoundInstance extends MovingSoundInstance {
        private final float basePitch;

        public final float reverbSend;
        public final float[] reverbParams;
        public final float[] eqParams;

        // 构造函数移除路径相关参数
        public ModSoundInstance(SoundEvent sound, BlockPos pos, float basePitch, float initVol, float initPitchMult,
                float reverbSend, float[] reverbParams, float[] eqParams) {
            super(sound, SoundCategory.RECORDS, Random.create());

            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;

            this.basePitch = basePitch;
            this.repeat = false;

            this.reverbSend = reverbSend;
            this.reverbParams = reverbParams != null ? reverbParams.clone() : ReverbDefinition.getDefault();
            this.eqParams = eqParams != null ? eqParams.clone() : FilterDefinition.getDefault();

            this.updateState(initVol, initPitchMult);
        }

        public void updateState(float v, float pMultiplier) {
            this.volume = Math.max(0.0f, Math.min(v, 1.0f));
            this.pitch = Math.max(0.01f, Math.min(this.basePitch * pMultiplier, 20.0f));
        }

        public void finish() {
            this.setDone();
        }

        // 允许外部设置位置
        public void setPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void tick() {
            // Tick 逻辑已由服务端驱动更新
        }
    }

    /**
     * 客户端声音淡入淡出器，用于处理预览声音的音量和音高曲线
     */
    public static class ClientSoundFader {
        private final UUID uuid;
        private final BlockPos pos;
        private final float baseVolume;
        private final List<Integer> volumeCurve;
        private final List<Integer> pitchCurve;
        private final int pitchRange;

        private final boolean motionMode;
        private final List<Vec3d> motionPath;
        private final int startTick;
        private final int endTick;

        private final double originX;
        private final double originY;
        private final double originZ;

        private int currentTick = 0;

        public ClientSoundFader(UUID uuid, BlockPos pos, float baseVolume,
                List<Integer> volumeCurve, List<Integer> pitchCurve,
                int pitchRange,
                boolean motionMode, List<Vec3d> motionPath, int startTick, int endTick) {
            this.uuid = uuid;
            this.pos = pos;
            this.baseVolume = baseVolume;
            this.volumeCurve = volumeCurve;
            this.pitchCurve = pitchCurve;
            this.pitchRange = pitchRange;

            this.motionMode = motionMode;
            this.motionPath = motionPath;
            this.startTick = startTick;
            this.endTick = endTick;

            this.originX = pos.getX() + 0.5;
            this.originY = pos.getY() + 0.5;
            this.originZ = pos.getZ() + 0.5;
        }

        /**
         * 每tick更新声音状态
         * 
         * @return 如果声音应该结束返回true
         */
        public boolean tick() {
            if (volumeCurve == null || currentTick >= volumeCurve.size()) {
                return true;
            }

            // 1. 计算音量
            int volRaw = volumeCurve.get(currentTick);
            float volume = (volRaw / 100.0f) * baseVolume;

            // 2. 计算音高倍率
            int pitchRaw = 0;
            if (pitchCurve != null && currentTick < pitchCurve.size()) {
                pitchRaw = pitchCurve.get(currentTick);
            }
            double semitones = pitchRaw / 100.0;
            float pitchMultiplier = (float) Math.pow(2.0, semitones / 12.0);

            // 3. 计算当前位置
            double currentX = originX;
            double currentY = originY;
            double currentZ = originZ;

            if (motionPath != null && !motionPath.isEmpty() && currentTick >= startTick && currentTick <= endTick) {
                int index = currentTick - startTick;
                if (index >= 0 && index < motionPath.size()) {
                    Vec3d offset = motionPath.get(index);
                    if (motionMode) {
                        currentX = originX + offset.x;
                        currentY = originY + offset.y;
                        currentZ = originZ + offset.z;
                    } else {
                        currentX = offset.x;
                        currentY = offset.y;
                        currentZ = offset.z;
                    }
                }
            }

            // 4. 更新声音状态
            updateSoundState(uuid, volume, pitchMultiplier, currentX, currentY, currentZ);

            currentTick++;
            return false;
        }
    }
}