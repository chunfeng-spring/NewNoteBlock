package com.chunfeng.newnoteblock.audio.manager;

import com.chunfeng.newnoteblock.audio.engine.NoteBlockAudioEngine;
import com.chunfeng.newnoteblock.audio.data.ReverbDefinition;
import com.chunfeng.newnoteblock.audio.data.FilterDefinition;
import com.chunfeng.newnoteblock.audio.manager.SamplerManager.SampleResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSoundManager {
    private static final Map<UUID, ModSoundInstance> activeSounds = new ConcurrentHashMap<>();

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
}