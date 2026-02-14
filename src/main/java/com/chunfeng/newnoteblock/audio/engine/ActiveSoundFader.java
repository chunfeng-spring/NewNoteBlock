package com.chunfeng.newnoteblock.audio.engine;

import com.chunfeng.newnoteblock.network.NotePacketHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

public class ActiveSoundFader {
    private final ServerWorld world;
    private final BlockPos pos;
    private final UUID uuid;
    private final float baseVolume;
    private final List<Integer> volumeCurve;
    private final List<Integer> pitchCurve;
    private final int pitchRange;

    // [新增] 移动逻辑所需的字段
    private final boolean motionMode;
    private final List<Vec3d> motionPath;
    private final int startTick;
    private final int endTick;

    // 预计算的中心点 (0.5, 0.5, 0.5)
    private final double originX;
    private final double originY;
    private final double originZ;

    private int currentTick = 0;
    private boolean isFinished = false;

    public ActiveSoundFader(ServerWorld world, BlockPos pos, UUID uuid, float baseVolume,
            List<Integer> volumeCurve, List<Integer> pitchCurve,
            int pitchRange,
            boolean motionMode, List<Vec3d> motionPath, int startTick, int endTick) { // [Modified] Added motion params
        this.world = world;
        this.pos = pos;
        this.uuid = uuid;
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

    public boolean tick() {
        boolean hasVolumeCurve = (volumeCurve != null && !volumeCurve.isEmpty());
        boolean hasMotion = (motionPath != null && !motionPath.isEmpty());

        // [修复] 结束条件：
        // - 有音量曲线时，以音量曲线长度为声音持续时间（音量曲线结束 = 声音结束）
        // - 无音量曲线但有运动路径时，以运动路径结束为 Fader 结束
        // - 都没有时，立即结束
        boolean finished;
        if (hasVolumeCurve) {
            finished = currentTick >= volumeCurve.size();
        } else if (hasMotion) {
            finished = currentTick > endTick;
        } else {
            finished = true;
        }

        if (finished) {
            isFinished = true;
            return true;
        }

        // 1. 计算音量 (如果有音量曲线)
        float volume = baseVolume;
        if (volumeCurve != null && !volumeCurve.isEmpty() && currentTick < volumeCurve.size()) {
            int volRaw = volumeCurve.get(currentTick);
            volume = (volRaw / 100.0f) * baseVolume;
        }

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

        // 4. 发送更新包
        NotePacketHandler.sendUpdateSoundState(uuid, volume, pitchMultiplier, world, pos, currentX, currentY, currentZ);

        currentTick++;
        return false;
    }

    public void stop() {
        isFinished = true;
        NotePacketHandler.sendStopSound(uuid, world, pos);
    }

    public UUID getUuid() {
        return uuid;
    }

    public BlockPos getPos() {
        return pos;
    }
}