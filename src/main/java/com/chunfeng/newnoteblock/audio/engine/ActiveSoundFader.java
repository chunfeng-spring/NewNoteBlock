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
        if (volumeCurve == null || currentTick >= volumeCurve.size()) {
            isFinished = true;
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

        // 3. 计算当前位置 [新增]
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

        // 4. 发送更新包 [Modified] 发送包含位置的更新包
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