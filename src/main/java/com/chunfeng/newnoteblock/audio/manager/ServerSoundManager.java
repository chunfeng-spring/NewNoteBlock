package com.chunfeng.newnoteblock.audio.manager;

import com.chunfeng.newnoteblock.audio.engine.ActiveSoundFader;
import com.chunfeng.newnoteblock.network.NotePacketHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSoundManager {
    private static final Map<UUID, ActiveSoundFader> activeSounds = new ConcurrentHashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (activeSounds.isEmpty())
                return;
            activeSounds.entrySet().removeIf(entry -> {
                ActiveSoundFader fader = entry.getValue();
                boolean finished = fader.tick();
                if (finished)
                    fader.stop();
                return finished;
            });
        });
    }

    public static void playSound(ServerWorld world, BlockPos pos, String instrument, int note, float baseVolume, // [Modified]
                                                                                                                 // Added
                                                                                                                 // baseVolume
            List<Integer> volCurve, List<Integer> pitchCurve,
            int pitchRange,
            float reverbSend, float[] reverbParams,
            float[] eqParams,
            int startTick, int endTick, boolean motionMode, List<Vec3d> motionPath) { // [Modified]

        stopSoundAt(pos);

        UUID uuid = UUID.randomUUID();

        boolean hasEnvelope = (volCurve != null && !volCurve.isEmpty());
        // [修复] 即使没有音量曲线，只要有运动路径也需要创建 Fader
        boolean hasMotion = (motionPath != null && !motionPath.isEmpty());

        float initVol = baseVolume;
        float initPitchMult = 1.0f;

        if (hasEnvelope) {
            initVol = (volCurve.get(0) / 100.0f) * baseVolume;

            if (pitchCurve != null && !pitchCurve.isEmpty()) {
                int pVal = pitchCurve.get(0);
                double semitones = pVal / 100.0;
                initPitchMult = (float) Math.pow(2.0, semitones / 12.0);
            }
        }

        // [修复] 如果有音量包络或运动路径，都需要创建 Fader
        if (hasEnvelope || hasMotion) {
            ActiveSoundFader fader = new ActiveSoundFader(world, pos, uuid, baseVolume, volCurve, pitchCurve,
                    pitchRange, motionMode, motionPath, startTick, endTick);
            activeSounds.put(uuid, fader);
        }

        final float finalVol = initVol;
        final float finalPitchMult = initPitchMult;

        world.getPlayers(p -> p.squaredDistanceTo(pos.toCenterPos()) < 64 * 64).forEach(p -> {
            NotePacketHandler.sendPlaySound(p, pos, uuid, instrument, note, finalVol, finalPitchMult,
                    pitchRange, reverbSend, reverbParams, eqParams, startTick, endTick, motionMode, motionPath);
        });
    }

    public static void stopSoundAt(BlockPos pos) {
        activeSounds.values().stream()
                .filter(f -> f.getPos().equals(pos))
                .forEach(ActiveSoundFader::stop);
    }
}