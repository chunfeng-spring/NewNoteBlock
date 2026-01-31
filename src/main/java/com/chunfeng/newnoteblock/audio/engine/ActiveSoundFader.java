package com.chunfeng.newnoteblock.audio.engine;

import com.chunfeng.newnoteblock.network.NotePacketHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public class ActiveSoundFader {
    private final ServerWorld world;
    private final BlockPos pos;
    private final UUID uuid;
    private final float baseVolume; // [Modified] Added baseVolume
    private final List<Integer> volumeCurve;
    private final List<Integer> pitchCurve;
    private final int pitchRange; // 弯音范围 (仅用于 GUI，引擎直接处理音分)

    private int currentTick = 0;
    private boolean isFinished = false;

    public ActiveSoundFader(ServerWorld world, BlockPos pos, UUID uuid, float baseVolume, // [Modified] Added baseVolume
            List<Integer> volumeCurve, List<Integer> pitchCurve,
            int pitchRange) {
        this.world = world;
        this.pos = pos;
        this.uuid = uuid;
        this.baseVolume = baseVolume; // [Modified]
        this.volumeCurve = volumeCurve;
        this.pitchCurve = pitchCurve;
        this.pitchRange = pitchRange;
    }

    public boolean tick() {
        // 如果曲线为空或走完，结束
        if (volumeCurve == null || currentTick >= volumeCurve.size()) {
            isFinished = true;
            return true;
        }

        // 1. 计算音量
        int volRaw = volumeCurve.get(currentTick);
        float volume = (volRaw / 100.0f) * baseVolume; // [Modified] Apply baseVolume

        // 2. 计算音高倍率
        // [修改] pitchRaw 现在直接代表 Cents (音分)
        // 范围: -Range*100 到 +Range*100
        int pitchRaw = 0; // 默认 0 音分
        // 保护：防止 pitchCurve 长度比 volumeCurve 短
        if (pitchCurve != null && currentTick < pitchCurve.size()) {
            pitchRaw = pitchCurve.get(currentTick);
        }

        // semitones: 偏移的半音数 (音分 / 100)
        double semitones = pitchRaw / 100.0;

        // multiplier: 最终倍率 = 2 ^ (半音数 / 12)
        float pitchMultiplier = (float) Math.pow(2.0, semitones / 12.0);

        // 3. 发送更新包
        NotePacketHandler.sendUpdateSoundState(uuid, volume, pitchMultiplier, world, pos);

        currentTick++;
        return false;
    }

    public void stop() {
        isFinished = true;
        // 只有 Fader 控制的声音才需要显式停止
        NotePacketHandler.sendStopSound(uuid, world, pos);
    }

    public UUID getUuid() {
        return uuid;
    }

    public BlockPos getPos() {
        return pos;
    }
}