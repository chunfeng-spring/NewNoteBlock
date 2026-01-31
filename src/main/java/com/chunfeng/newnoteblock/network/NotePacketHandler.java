package com.chunfeng.newnoteblock.network;

import com.chunfeng.newnoteblock.NewNoteBlockMod;
import com.chunfeng.newnoteblock.block.NewNoteBlockEntity;
import com.chunfeng.newnoteblock.audio.manager.ClientSoundManager;
import com.chunfeng.newnoteblock.audio.data.ReverbDefinition;
import com.chunfeng.newnoteblock.audio.data.FilterDefinition;
import com.chunfeng.newnoteblock.util.InstrumentBlockRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotePacketHandler {
    public static final Identifier UPDATE_NOTE_PACKET = new Identifier(NewNoteBlockMod.MOD_ID, "update_note");
    public static final Identifier PLAY_SOUND_PACKET = new Identifier(NewNoteBlockMod.MOD_ID, "play_sound");
    public static final Identifier UPDATE_SOUND_STATE_PACKET = new Identifier(NewNoteBlockMod.MOD_ID,
            "update_sound_state");
    public static final Identifier STOP_SOUND_PACKET = new Identifier(NewNoteBlockMod.MOD_ID, "stop_sound");

    public static void registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_NOTE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    int note = buf.readInt();
                    String instrument = buf.readString();
                    float volume = buf.readFloat(); // [Modified] Read volume

                    int volSize = buf.readInt();
                    List<Integer> volCurve = new ArrayList<>();
                    for (int i = 0; i < volSize; i++)
                        volCurve.add(buf.readInt());

                    int pitchSize = buf.readInt();
                    List<Integer> pitchCurve = new ArrayList<>();
                    for (int i = 0; i < pitchSize; i++)
                        pitchCurve.add(buf.readInt());

                    int pitchRange = buf.readInt();
                    int delay = buf.readInt();

                    float reverbSend = buf.readFloat();
                    float[] rParams = new float[ReverbDefinition.PARAM_COUNT];
                    for (int i = 0; i < rParams.length; i++)
                        rParams[i] = buf.readFloat();

                    float[] eParams = new float[FilterDefinition.PARAM_COUNT];
                    for (int i = 0; i < eParams.length; i++)
                        eParams[i] = buf.readFloat();

                    // 读取运动表达式
                    String expX = buf.readString();
                    String expY = buf.readString();
                    String expZ = buf.readString();
                    int startTick = buf.readInt();
                    int endTick = buf.readInt();
                    boolean motionMode = buf.readBoolean(); // [新增]

                    // [新增] 读取预计算的轨迹数据
                    int pathSize = buf.readInt();
                    List<Vec3d> motionPath = new ArrayList<>();
                    for (int i = 0; i < pathSize; i++) {
                        motionPath.add(new Vec3d(buf.readFloat(), buf.readFloat(), buf.readFloat()));
                    }

                    server.execute(() -> {
                        if (player.getWorld() != null && player.getWorld().isChunkLoaded(pos)) {
                            BlockEntity be = player.getWorld().getBlockEntity(pos);
                            if (be instanceof NewNoteBlockEntity noteBe) {
                                String currentInstrument = noteBe.getInstrument();
                                if (!currentInstrument.equals(instrument)) {
                                    BlockState targetState = InstrumentBlockRegistry
                                            .getBlockStateFromInstrument(instrument);
                                    BlockPos downPos = pos.down();
                                    player.getWorld().setBlockState(downPos, targetState, 3);
                                }
                                // 更新数据
                                noteBe.updateData(note, instrument, volume, volCurve, pitchCurve, pitchRange, delay,
                                        reverbSend,
                                        rParams, eParams, expX, expY, expZ, startTick, endTick, motionMode, motionPath);
                            }
                        }
                    });
                });
    }

    public static void registerClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(PLAY_SOUND_PACKET, (client, handler, buf, responseSender) -> {
            UUID id = buf.readUuid();
            BlockPos pos = buf.readBlockPos();
            String instrument = buf.readString();
            int note = buf.readInt();
            float initVol = buf.readFloat();
            float initPitchMult = buf.readFloat();
            int pitchRange = buf.readInt();
            float reverbSend = buf.readFloat();

            float[] rParams = new float[ReverbDefinition.PARAM_COUNT];
            for (int i = 0; i < rParams.length; i++)
                rParams[i] = buf.readFloat();

            float[] eParams = new float[FilterDefinition.PARAM_COUNT];
            for (int i = 0; i < eParams.length; i++)
                eParams[i] = buf.readFloat();

            // 读取运动参数
            int startTick = buf.readInt();
            int endTick = buf.readInt();
            boolean motionMode = buf.readBoolean(); // [新增]

            // [新增] 读取轨迹数据
            int pathSize = buf.readInt();
            List<Vec3d> motionPath = new ArrayList<>();
            for (int i = 0; i < pathSize; i++) {
                motionPath.add(new Vec3d(buf.readFloat(), buf.readFloat(), buf.readFloat()));
            }

            client.execute(() -> ClientSoundManager.startSound(
                    id, pos, instrument, note, initVol, initPitchMult, pitchRange,
                    reverbSend, rParams, eParams,
                    startTick, endTick, motionMode, motionPath // 传递轨迹数据
            ));
        });

        // ... (Update 和 Stop 保持不变)
        ClientPlayNetworking.registerGlobalReceiver(UPDATE_SOUND_STATE_PACKET,
                (client, handler, buf, responseSender) -> {
                    UUID id = buf.readUuid();
                    float volume = buf.readFloat();
                    float pitchMultiplier = buf.readFloat();
                    client.execute(() -> ClientSoundManager.updateSoundState(id, volume, pitchMultiplier));
                });

        ClientPlayNetworking.registerGlobalReceiver(STOP_SOUND_PACKET, (client, handler, buf, responseSender) -> {
            UUID id = buf.readUuid();
            client.execute(() -> ClientSoundManager.stopSound(id));
        });
    }

    public static void sendPlaySound(ServerPlayerEntity player, BlockPos pos, UUID uuid, String instrument, int note,
            float initVolume, float initPitchMultiplier,
            int pitchRange,
            float reverbSend, float[] reverbParams,
            float[] eqParams,
            int startTick, int endTick, boolean motionMode, List<Vec3d> motionPath) { // [新增参数]
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(uuid);
        buf.writeBlockPos(pos);
        buf.writeString(instrument);
        buf.writeInt(note);
        buf.writeFloat(initVolume);
        buf.writeFloat(initPitchMultiplier);
        buf.writeInt(pitchRange);
        buf.writeFloat(reverbSend);

        if (reverbParams == null || reverbParams.length < ReverbDefinition.PARAM_COUNT)
            reverbParams = ReverbDefinition.getDefault();
        for (float param : reverbParams)
            buf.writeFloat(param);

        if (eqParams == null || eqParams.length < FilterDefinition.PARAM_COUNT)
            eqParams = FilterDefinition.getDefault();
        for (float param : eqParams)
            buf.writeFloat(param);

        // [新增] 写入运动数据
        buf.writeInt(startTick);
        buf.writeInt(endTick);
        buf.writeBoolean(motionMode); // [新增]

        if (motionPath == null)
            motionPath = new ArrayList<>();
        buf.writeInt(motionPath.size());
        for (Vec3d vec : motionPath) {
            buf.writeFloat((float) vec.x);
            buf.writeFloat((float) vec.y);
            buf.writeFloat((float) vec.z);
        }

        ServerPlayNetworking.send(player, PLAY_SOUND_PACKET, buf);
    }

    // ... (sendUpdateSoundState 和 sendStopSound 保持不变)
    public static void sendUpdateSoundState(UUID uuid, float volume, float pitchMultiplier, ServerWorld world,
            BlockPos pos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(uuid);
        buf.writeFloat(volume);
        buf.writeFloat(pitchMultiplier);
        world.getPlayers(p -> p.squaredDistanceTo(pos.toCenterPos()) < 64 * 64)
                .forEach(player -> ServerPlayNetworking.send(player, UPDATE_SOUND_STATE_PACKET, buf));
    }

    public static void sendStopSound(UUID uuid, ServerWorld world, BlockPos pos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(uuid);
        world.getPlayers(p -> p.squaredDistanceTo(pos.toCenterPos()) < 64 * 64)
                .forEach(player -> ServerPlayNetworking.send(player, STOP_SOUND_PACKET, buf));
    }
}