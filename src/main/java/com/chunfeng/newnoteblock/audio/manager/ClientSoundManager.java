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

    // [修改] 接收预计算的 path 数据
    public static void startSound(UUID id, BlockPos pos, String instrumentId, int note,
            float initVolume, float initPitchMult,
            int pitchRange,
            float reverbSend, float[] reverbParams,
            float[] eqParams,
            int startTick, int endTick, boolean motionMode, List<Vec3d> motionPath) { // [新增参数]
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
                reverbSend, reverbParams, eqParams,
                startTick, endTick, motionMode, motionPath // 传递 boolean
        );
        activeSounds.put(id, instance);

        client.execute(() -> {
            client.getSoundManager().play(instance);
        });
    }

    public static void updateSoundState(UUID id, float volume, float pitchMultiplier) {
        ModSoundInstance instance = activeSounds.get(id);
        if (instance != null)
            instance.updateState(volume, pitchMultiplier);
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

    // ...

    public static class ModSoundInstance extends MovingSoundInstance {
        private final float basePitch;

        public final float reverbSend;
        public final float[] reverbParams;
        public final float[] eqParams;

        // [修改] 存储预计算轨迹
        private final double originX, originY, originZ;
        private final int startTick, endTick;
        private final boolean motionMode; // [新增]
        private final List<Vec3d> motionPath;
        private int currentTick = 0;

        public ModSoundInstance(SoundEvent sound, BlockPos pos, float basePitch, float initVol, float initPitchMult,
                float reverbSend, float[] reverbParams, float[] eqParams,
                int start, int end, boolean mMode, List<Vec3d> path) { // [新增包含]
            super(sound, SoundCategory.RECORDS, Random.create());
            this.originX = pos.getX() + 0.5;
            this.originY = pos.getY() + 0.5;
            this.originZ = pos.getZ() + 0.5;
            this.x = originX;
            this.y = originY;
            this.z = originZ;

            this.basePitch = basePitch;
            this.repeat = false;

            this.reverbSend = reverbSend;
            this.reverbParams = reverbParams != null ? reverbParams.clone() : ReverbDefinition.getDefault();
            this.eqParams = eqParams != null ? eqParams.clone() : FilterDefinition.getDefault();

            this.startTick = start;
            this.endTick = end;
            this.motionMode = mMode;
            this.motionPath = path;

            // [Fix] 立即计算初始位置(Tick 0)，防止第一帧位置错误
            updateCurrentPosition();

            this.updateState(initVol, initPitchMult);
        }

        public void updateState(float v, float pMultiplier) {
            this.volume = Math.max(0.0f, Math.min(v, 1.0f));
            this.pitch = Math.max(0.01f, Math.min(this.basePitch * pMultiplier, 20.0f));
        }

        public void finish() {
            this.setDone();
        }

        @Override
        public void tick() {
            if (this.isDone())
                return;

            updateCurrentPosition();

            currentTick++;
        }

        private void updateCurrentPosition() {
            // [性能优化] 直接查表，无需表达式计算
            if (motionPath != null && !motionPath.isEmpty() && currentTick >= startTick && currentTick <= endTick) {
                int index = currentTick - startTick;
                if (index >= 0 && index < motionPath.size()) {
                    Vec3d offset = motionPath.get(index);
                    if (motionMode) {
                        // 相对坐标
                        this.x = originX + offset.x;
                        this.y = originY + offset.y;
                        this.z = originZ + offset.z;
                    } else {
                        // 绝对坐标
                        this.x = offset.x;
                        this.y = offset.y;
                        this.z = offset.z;
                    }
                }
            } else if (currentTick > endTick || currentTick < startTick) {
                // 定义域外恢复默认位置
                this.x = originX;
                this.y = originY;
                this.z = originZ;
            }
        }
    }
}