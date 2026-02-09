package com.chunfeng.newnoteblock.network;

import com.chunfeng.newnoteblock.NewNoteBlockMod;
import com.chunfeng.newnoteblock.block.NewNoteBlockEntity;
import com.chunfeng.newnoteblock.client.ui.screen.WorldEditScreen;
import com.chunfeng.newnoteblock.audio.data.FilterDefinition;
import com.chunfeng.newnoteblock.audio.data.ReverbDefinition;
import com.chunfeng.newnoteblock.util.InstrumentBlockRegistry;
import com.chunfeng.newnoteblock.util.WEReflection;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.chunfeng.newnoteblock.util.MotionCalculator;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WEPacketHandler {
    public static final Identifier WE_REQUEST_OPEN_GUI_PACKET = new Identifier(NewNoteBlockMod.MOD_ID,
            "request_open_we_gui");
    public static final Identifier WE_OPEN_GUI_PACKET = new Identifier(NewNoteBlockMod.MOD_ID, "open_we_gui");
    public static final Identifier WE_UPDATE_PACKET = new Identifier(NewNoteBlockMod.MOD_ID, "we_update_full");

    // --- 数据结构 ---

    public static class FilterRule {
        public enum Type {
            INSTRUMENT, NOTE
        }

        public enum Logic {
            AND, OR
        }

        public Type type;
        public Logic logic;
        public boolean invert;
        public String instrumentId;
        public int minNote, maxNote;

        public FilterRule() {
        }

        public FilterRule(Logic l, boolean inv, String inst) {
            this.type = Type.INSTRUMENT;
            this.logic = l;
            this.invert = inv;
            this.instrumentId = inst;
        }

        public FilterRule(Logic l, boolean inv, int min, int max) {
            this.type = Type.NOTE;
            this.logic = l;
            this.invert = inv;
            this.minNote = min;
            this.maxNote = max;
        }
    }

    public static class FilterOptions {
        public List<FilterRule> rules = new ArrayList<>();
    }

    public static class UpdateMask {
        // 绝对值覆盖
        public boolean updateInstrument;
        public boolean updateNote;
        public boolean updateVolumeCurve;
        public boolean updatePitchCurve;
        public boolean updatePitchRange;
        public boolean updateDelay;
        public boolean updateReverb;
        public boolean updateEq;
        public boolean updateMotion;

        // 相对值偏移操作
        public boolean shiftNote;
        public boolean shiftVolumeEnv; // [Renamed]
        public boolean shiftMasterVolume; // [New]
        public boolean shiftDelay;
        public boolean shiftReverbSend;
        public boolean shiftReverbGain; // [New]

        public boolean shiftMotionX; // [New]
        public boolean shiftMotionY; // [New]
        public boolean shiftMotionZ; // [New]

        public boolean updateMasterVolume; // [New]
    }

    public static class DataPayload {
        // 绝对值数据
        public String instrument;
        public int note;
        public float volume; // [New]
        public List<Integer> volCurve;
        public List<Integer> pitchCurve;
        public int pitchRange;
        public int delay;
        public float reverbSend;
        public float[] reverbParams;
        public float[] eqParams;
        public String expX, expY, expZ;
        public int startTick, endTick;
        public boolean motionMode; // [New]
        public List<Vec3d> motionPath;

        // 相对值偏移数据
        public float shiftNoteVal; // [Modified] float
        public int shiftNoteOp;

        public float shiftVolumeEnvVal; // [Modified] float
        public int shiftVolumeEnvOp;

        public float shiftMasterVolumeVal;
        public int shiftMasterVolumeOp;

        public float shiftDelayVal; // [Modified] float
        public int shiftDelayOp;

        public float shiftReverbSendVal;
        public int shiftReverbOp; // [New]

        public float shiftReverbGainVal; // [New]
        public int shiftReverbGainOp;

        public float shiftMotionXVal; // [New]
        public int shiftMotionXOp;

        public float shiftMotionYVal; // [New]
        public int shiftMotionYOp;

        public float shiftMotionZVal; // [New]
        public int shiftMotionZOp;
    }

    // --- 注册逻辑 (保持不变) ---
    public static void registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(WE_UPDATE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    // 1. Filter
                    FilterOptions filter = new FilterOptions();
                    int ruleCount = buf.readInt();
                    for (int i = 0; i < ruleCount; i++) {
                        FilterRule rule = new FilterRule();
                        rule.type = FilterRule.Type.values()[buf.readInt()];
                        rule.logic = FilterRule.Logic.values()[buf.readInt()];
                        rule.invert = buf.readBoolean();
                        if (rule.type == FilterRule.Type.INSTRUMENT) {
                            rule.instrumentId = buf.readString();
                        } else {
                            rule.minNote = buf.readInt();
                            rule.maxNote = buf.readInt();
                        }
                        filter.rules.add(rule);
                    }

                    // 2. Mask
                    UpdateMask mask = new UpdateMask();
                    mask.updateInstrument = buf.readBoolean();
                    mask.updateNote = buf.readBoolean();
                    mask.updateVolumeCurve = buf.readBoolean();
                    mask.updatePitchCurve = buf.readBoolean();
                    mask.updatePitchRange = buf.readBoolean();
                    mask.updateDelay = buf.readBoolean();
                    mask.updateReverb = buf.readBoolean();
                    mask.updateEq = buf.readBoolean();
                    mask.updateMotion = buf.readBoolean();
                    mask.shiftNote = buf.readBoolean();
                    mask.shiftVolumeEnv = buf.readBoolean(); // [Renamed]
                    mask.shiftMasterVolume = buf.readBoolean(); // [New]
                    mask.shiftDelay = buf.readBoolean();
                    mask.shiftReverbSend = buf.readBoolean();
                    mask.shiftReverbGain = buf.readBoolean(); // [New]
                    mask.shiftMotionX = buf.readBoolean(); // [New]
                    mask.shiftMotionY = buf.readBoolean(); // [New]
                    mask.shiftMotionZ = buf.readBoolean(); // [New]
                    mask.updateMasterVolume = buf.readBoolean(); // [New]

                    // 3. Payload
                    DataPayload data = new DataPayload();
                    data.instrument = buf.readString();
                    data.note = buf.readInt();
                    data.volume = buf.readFloat(); // [New]

                    int volSize = buf.readInt();
                    data.volCurve = new ArrayList<>();
                    for (int i = 0; i < volSize; i++)
                        data.volCurve.add(buf.readInt());

                    int pitchSize = buf.readInt();
                    data.pitchCurve = new ArrayList<>();
                    for (int i = 0; i < pitchSize; i++)
                        data.pitchCurve.add(buf.readInt());

                    data.pitchRange = buf.readInt();
                    data.delay = buf.readInt();

                    data.reverbSend = buf.readFloat();
                    data.reverbParams = new float[ReverbDefinition.PARAM_COUNT];
                    for (int i = 0; i < data.reverbParams.length; i++)
                        data.reverbParams[i] = buf.readFloat();

                    data.eqParams = new float[FilterDefinition.PARAM_COUNT];
                    for (int i = 0; i < data.eqParams.length; i++)
                        data.eqParams[i] = buf.readFloat();

                    data.expX = buf.readString();
                    data.expY = buf.readString();
                    data.expZ = buf.readString();
                    data.startTick = buf.readInt();
                    data.endTick = buf.readInt();
                    data.motionMode = buf.readBoolean(); // [New]

                    int pathSize = buf.readInt();
                    data.motionPath = new ArrayList<>();
                    for (int i = 0; i < pathSize; i++) {
                        data.motionPath.add(new Vec3d(buf.readFloat(), buf.readFloat(), buf.readFloat()));
                    }

                    data.shiftNoteVal = buf.readFloat(); // [Modified]
                    data.shiftNoteOp = buf.readInt();

                    data.shiftVolumeEnvVal = buf.readFloat(); // [Modified]
                    data.shiftVolumeEnvOp = buf.readInt();

                    data.shiftMasterVolumeVal = buf.readFloat();
                    data.shiftMasterVolumeOp = buf.readInt();

                    data.shiftDelayVal = buf.readFloat(); // [Modified]
                    data.shiftDelayOp = buf.readInt();

                    data.shiftReverbSendVal = buf.readFloat();
                    data.shiftReverbOp = buf.readInt();

                    data.shiftReverbGainVal = buf.readFloat(); // [New]
                    data.shiftReverbGainOp = buf.readInt();

                    data.shiftMotionXVal = buf.readFloat(); // [New]
                    data.shiftMotionXOp = buf.readInt();

                    data.shiftMotionYVal = buf.readFloat(); // [New]
                    data.shiftMotionYOp = buf.readInt();

                    data.shiftMotionZVal = buf.readFloat(); // [New]
                    data.shiftMotionZOp = buf.readInt();

                    server.execute(() -> handleWEUpdate(player, filter, mask, data));
                });

        ServerPlayNetworking.registerGlobalReceiver(WE_REQUEST_OPEN_GUI_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    server.execute(() -> handleOpenGuiRequest(player));
                });
    }

    public static void registerClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(WE_OPEN_GUI_PACKET, (client, handler, buf, responseSender) -> {
            long selectionVolume = buf.readLong();
            client.execute(() -> {
                // [新增] 如果正在 Replay 回放中，不打开 GUI
                if (com.chunfeng.newnoteblock.util.ReplayModCompat.isInReplay()) {
                    return;
                }
                WorldEditScreen.open(selectionVolume);
            });
        });
    }

    public static void requestOpenGui() {
        ClientPlayNetworking.send(WE_REQUEST_OPEN_GUI_PACKET, PacketByteBufs.create());
    }

    public static void sendWEUpdate(FilterOptions filter, UpdateMask mask, DataPayload data) {
        PacketByteBuf buf = PacketByteBufs.create();

        // Write Filter
        buf.writeInt(filter.rules.size());
        for (FilterRule rule : filter.rules) {
            buf.writeInt(rule.type.ordinal());
            buf.writeInt(rule.logic.ordinal());
            buf.writeBoolean(rule.invert);
            if (rule.type == FilterRule.Type.INSTRUMENT) {
                buf.writeString(rule.instrumentId);
            } else {
                buf.writeInt(rule.minNote);
                buf.writeInt(rule.maxNote);
            }
        }

        // Write Mask
        buf.writeBoolean(mask.updateInstrument);
        buf.writeBoolean(mask.updateNote);
        buf.writeBoolean(mask.updateVolumeCurve);
        buf.writeBoolean(mask.updatePitchCurve);
        buf.writeBoolean(mask.updatePitchRange);
        buf.writeBoolean(mask.updateDelay);
        buf.writeBoolean(mask.updateReverb);
        buf.writeBoolean(mask.updateEq);
        buf.writeBoolean(mask.updateMotion);
        buf.writeBoolean(mask.shiftNote);
        buf.writeBoolean(mask.shiftVolumeEnv); // [Renamed]
        buf.writeBoolean(mask.shiftMasterVolume); // [New]
        buf.writeBoolean(mask.shiftDelay);
        buf.writeBoolean(mask.shiftReverbSend);
        buf.writeBoolean(mask.shiftReverbGain); // [New]
        buf.writeBoolean(mask.shiftMotionX); // [New]
        buf.writeBoolean(mask.shiftMotionY); // [New]
        buf.writeBoolean(mask.shiftMotionZ); // [New]
        buf.writeBoolean(mask.updateMasterVolume); // [New]

        // Write Data
        buf.writeString(data.instrument);
        buf.writeInt(data.note);
        buf.writeFloat(data.volume); // [New]
        buf.writeInt(data.volCurve.size());
        for (Integer v : data.volCurve)
            buf.writeInt(v);
        buf.writeInt(data.pitchCurve.size());
        for (Integer v : data.pitchCurve)
            buf.writeInt(v);
        buf.writeInt(data.pitchRange);
        buf.writeInt(data.delay);
        buf.writeFloat(data.reverbSend);
        for (float f : data.reverbParams)
            buf.writeFloat(f);
        for (float f : data.eqParams)
            buf.writeFloat(f);
        buf.writeString(data.expX);
        buf.writeString(data.expY);
        buf.writeString(data.expZ);
        buf.writeInt(data.startTick);
        buf.writeInt(data.endTick);
        buf.writeBoolean(data.motionMode); // [New]
        buf.writeInt(data.motionPath.size());
        for (Vec3d v : data.motionPath) {
            buf.writeFloat((float) v.x);
            buf.writeFloat((float) v.y);
            buf.writeFloat((float) v.z);
        }
        buf.writeFloat(data.shiftNoteVal); // [Modified]
        buf.writeInt(data.shiftNoteOp);

        buf.writeFloat(data.shiftVolumeEnvVal); // [Modified]
        buf.writeInt(data.shiftVolumeEnvOp);

        buf.writeFloat(data.shiftMasterVolumeVal);
        buf.writeInt(data.shiftMasterVolumeOp);

        buf.writeFloat(data.shiftDelayVal); // [Modified]
        buf.writeInt(data.shiftDelayOp);

        buf.writeFloat(data.shiftReverbSendVal);
        buf.writeInt(data.shiftReverbOp);

        buf.writeFloat(data.shiftReverbGainVal); // [New]
        buf.writeInt(data.shiftReverbGainOp);

        buf.writeFloat(data.shiftMotionXVal); // [New]
        buf.writeInt(data.shiftMotionXOp);

        buf.writeFloat(data.shiftMotionYVal); // [New]
        buf.writeInt(data.shiftMotionYOp);

        buf.writeFloat(data.shiftMotionZVal); // [New]
        buf.writeInt(data.shiftMotionZOp);

        ClientPlayNetworking.send(WE_UPDATE_PACKET, buf);
    }

    private static void handleOpenGuiRequest(ServerPlayerEntity player) {
        Player wePlayer = WEReflection.getPlayer(player);
        if (wePlayer == null)
            return;
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
        com.sk89q.worldedit.world.World weWorld = WEReflection.getWorld(player.getServerWorld());

        try {
            Region region = session.getSelection(weWorld);
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeLong(region.getVolume());
            ServerPlayNetworking.send(player, WE_OPEN_GUI_PACKET, buf);
        } catch (IncompleteRegionException e) {
            player.sendMessage(Text.of("§c[NewNoteBlock] 请先使用小木斧选择一个区域。"), true);
        } catch (Exception e) {
            player.sendMessage(Text.of("§c[NewNoteBlock] 获取选区失败。"), false);
        }
    }

    private static boolean checkFilter(NewNoteBlockEntity be, FilterOptions options) {
        if (options.rules.isEmpty())
            return true;
        boolean result = false;
        for (int i = 0; i < options.rules.size(); i++) {
            FilterRule rule = options.rules.get(i);
            boolean ruleResult;
            if (rule.type == FilterRule.Type.INSTRUMENT) {
                ruleResult = be.getInstrument().equals(rule.instrumentId);
            } else {
                int n = be.getNote();
                ruleResult = (n >= rule.minNote && n <= rule.maxNote);
            }
            if (rule.invert)
                ruleResult = !ruleResult;
            if (i == 0)
                result = ruleResult;
            else {
                if (rule.logic == FilterRule.Logic.AND)
                    result = result && ruleResult;
                else
                    result = result || ruleResult;
            }
        }
        return result;
    }

    private static void handleWEUpdate(ServerPlayerEntity player, FilterOptions filter, UpdateMask mask,
            DataPayload data) {
        try {
            Player wePlayer = WEReflection.getPlayer(player);
            com.sk89q.worldedit.world.World weWorld = WEReflection.getWorld(player.getServerWorld());
            if (wePlayer == null || weWorld == null)
                return;

            LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
            Region region = session.getSelection(weWorld);
            if (region == null)
                return;

            int count = 0;
            ServerWorld world = player.getServerWorld();

            try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(weWorld)
                    .actor(wePlayer)
                    .build()) {

                for (BlockVector3 vec : region) {
                    BlockPos pos = new BlockPos(vec.getX(), vec.getY(), vec.getZ());
                    Chunk chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);

                    if (chunk != null) {
                        BlockEntity be = chunk.getBlockEntity(pos);
                        if (be instanceof NewNoteBlockEntity noteBe) {
                            if (!checkFilter(noteBe, filter))
                                continue;

                            // 1. 获取当前方块的 NBT (已包含最新的正确数据)
                            NbtCompound nbt = noteBe.createNbtWithId();
                            boolean changed = false;

                            // [移除] 之前错误的 "NBT清洗" 代码，该代码会导致未被修改的 EQ 数据损坏

                            // 2. 绝对值覆盖
                            if (mask.updateInstrument) {
                                nbt.putString("Instrument", data.instrument);
                                changed = true;
                            }
                            if (mask.updateNote) {
                                nbt.putInt("Note", data.note);
                                changed = true;
                            }
                            // [Modified] Master Volume Overwrite (Merged into VolumeCurve logic? No,
                            // separate)
                            // The user requested: "In absolute overwrite, volume envelope modification
                            // should include master volume modification"
                            if (mask.updateVolumeCurve) {
                                nbt.putIntArray("VolumeCurve", data.volCurve.stream().mapToInt(i -> i).toArray());
                                changed = true;
                            }
                            if (mask.updateMasterVolume) {
                                nbt.putFloat("Volume", data.volume);
                                changed = true;
                            }
                            if (mask.updatePitchCurve) {
                                nbt.putIntArray("PitchCurve", data.pitchCurve.stream().mapToInt(i -> i).toArray());
                                changed = true;
                            }
                            if (mask.updatePitchRange) {
                                nbt.putInt("PitchRange", data.pitchRange);
                                changed = true;
                            }
                            if (mask.updateDelay) {
                                nbt.putInt("Delay", data.delay);
                                changed = true;
                            }

                            if (mask.updateReverb) {
                                nbt.putFloat("ReverbSend", data.reverbSend);
                                int[] packed = new int[data.reverbParams.length];
                                for (int i = 0; i < data.reverbParams.length; i++)
                                    packed[i] = (int) (data.reverbParams[i] * 1000.0f);
                                nbt.putIntArray("ReverbParamsPacked", packed);
                                changed = true;
                            }

                            if (mask.updateEq) {
                                // [修复] 写入 List 格式 (优先)
                                NbtList list = new NbtList();
                                for (float f : data.eqParams) {
                                    list.add(NbtFloat.of(f));
                                }
                                nbt.put("EqParams", list);

                                // [修复] 同时写入 Packed 格式，使用正确的 x100 倍率 (之前是 1000.0f 导致错误)
                                int[] packed = new int[data.eqParams.length];
                                for (int i = 0; i < data.eqParams.length; i++)
                                    packed[i] = (int) (data.eqParams[i] * 100.0f);
                                nbt.putIntArray("EqParamsPacked", packed);

                                changed = true;
                            }

                            if (mask.updateMotion) {
                                nbt.putString("MotionExpX", data.expX);
                                nbt.putString("MotionExpY", data.expY);
                                nbt.putString("MotionExpZ", data.expZ);
                                nbt.putInt("MotionStartTick", data.startTick);
                                nbt.putInt("MotionEndTick", data.endTick);
                                nbt.putBoolean("MotionMode", data.motionMode); // [New]
                                int size = data.motionPath.size();
                                int[] xArr = new int[size];
                                int[] yArr = new int[size];
                                int[] zArr = new int[size];
                                for (int i = 0; i < size; i++) {
                                    Vec3d v = data.motionPath.get(i);
                                    xArr[i] = (int) (v.x * 1000.0);
                                    yArr[i] = (int) (v.y * 1000.0);
                                    zArr[i] = (int) (v.z * 1000.0);
                                }
                                nbt.putIntArray("MotionPathX", xArr);
                                nbt.putIntArray("MotionPathY", yArr);
                                nbt.putIntArray("MotionPathZ", zArr);
                                changed = true;
                            }

                            // 3. 相对值偏移
                            if (mask.shiftNote) {
                                int current = nbt.getInt("Note");
                                // [Modified] Use float op and round
                                float res = applyOpFloat(current, data.shiftNoteVal, data.shiftNoteOp, 0, 127);
                                nbt.putInt("Note", (int) Math.round(res));
                                changed = true;
                            }
                            // [Modified] Shift Volume Envelope
                            if (mask.shiftVolumeEnv) {
                                int[] curve = nbt.getIntArray("VolumeCurve");
                                if (curve.length > 0) {
                                    for (int i = 0; i < curve.length; i++) {
                                        float res = applyOpFloat(curve[i], data.shiftVolumeEnvVal,
                                                data.shiftVolumeEnvOp, 0, 100);
                                        curve[i] = (int) Math.round(res);
                                    }
                                    nbt.putIntArray("VolumeCurve", curve);
                                    changed = true;
                                }
                            }
                            // [New] Shift Master Volume
                            if (mask.shiftMasterVolume) {
                                float current = nbt.contains("Volume") ? nbt.getFloat("Volume") : 1.0f;
                                float newVal = applyOpFloat(current, data.shiftMasterVolumeVal,
                                        data.shiftMasterVolumeOp, 0.0f, 1.0f);
                                nbt.putFloat("Volume", newVal);
                                changed = true;
                            }
                            if (mask.shiftDelay) {
                                int current = nbt.getInt("Delay");
                                float res = applyOpFloat(current, data.shiftDelayVal, data.shiftDelayOp, 0, 5000);
                                nbt.putInt("Delay", (int) Math.round(res));
                                changed = true;
                            }
                            if (mask.shiftReverbSend) {
                                float current = nbt.getFloat("ReverbSend");
                                float newVal = applyOpFloat(current, data.shiftReverbSendVal, data.shiftReverbOp, 0.0f,
                                        1.0f);
                                nbt.putFloat("ReverbSend", newVal);
                                changed = true;
                            }

                            if (mask.shiftReverbGain) {
                                int[] packed = nbt.getIntArray("ReverbParamsPacked");
                                if (packed.length == ReverbDefinition.PARAM_COUNT) {
                                    // Index 2 is GAIN
                                    float current = packed[ReverbDefinition.GAIN] / 1000.0f;
                                    float newVal = applyOpFloat(current, data.shiftReverbGainVal,
                                            data.shiftReverbGainOp, 0.0f, 1.0f);
                                    packed[ReverbDefinition.GAIN] = (int) (newVal * 1000.0f);
                                    nbt.putIntArray("ReverbParamsPacked", packed);
                                    changed = true;
                                }
                            }

                            boolean motionExpChanged = false;
                            if (mask.shiftMotionX) {
                                String oldExp = nbt.contains("MotionExpX") ? nbt.getString("MotionExpX") : "0";
                                String newExp = applyOpString(oldExp, data.shiftMotionXVal, data.shiftMotionXOp);
                                nbt.putString("MotionExpX", newExp);
                                changed = true;
                                motionExpChanged = true;
                            }
                            if (mask.shiftMotionY) {
                                String oldExp = nbt.contains("MotionExpY") ? nbt.getString("MotionExpY") : "0";
                                String newExp = applyOpString(oldExp, data.shiftMotionYVal, data.shiftMotionYOp);
                                nbt.putString("MotionExpY", newExp);
                                changed = true;
                                motionExpChanged = true;
                            }
                            if (mask.shiftMotionZ) {
                                String oldExp = nbt.contains("MotionExpZ") ? nbt.getString("MotionExpZ") : "0";
                                String newExp = applyOpString(oldExp, data.shiftMotionZVal, data.shiftMotionZOp);
                                nbt.putString("MotionExpZ", newExp);
                                changed = true;
                                motionExpChanged = true;
                            }

                            // [New] Auto-calculate trajectory if expressions changed
                            if (motionExpChanged) {
                                try {
                                    String ex = nbt.getString("MotionExpX");
                                    String ey = nbt.getString("MotionExpY");
                                    String ez = nbt.getString("MotionExpZ");
                                    int start = nbt.getInt("MotionStartTick");
                                    int end = nbt.getInt("MotionEndTick");

                                    List<Vec3d> newPath = MotionCalculator.calculate(ex, ey, ez, start, end);

                                    int size = newPath.size();
                                    int[] xArr = new int[size];
                                    int[] yArr = new int[size];
                                    int[] zArr = new int[size];
                                    for (int i = 0; i < size; i++) {
                                        Vec3d v = newPath.get(i);
                                        xArr[i] = (int) (v.x * 1000.0);
                                        yArr[i] = (int) (v.y * 1000.0);
                                        zArr[i] = (int) (v.z * 1000.0);
                                    }
                                    nbt.putIntArray("MotionPathX", xArr);
                                    nbt.putIntArray("MotionPathY", yArr);
                                    nbt.putIntArray("MotionPathZ", zArr);

                                } catch (Exception e) {
                                    // Ignore calculation errors during batch update
                                }
                            }

                            if (changed) {
                                BlockState mcState = noteBe.getCachedState();
                                com.sk89q.worldedit.world.block.BlockState weState = FabricAdapter.adapt(mcState);
                                CompoundTag weNbtTag = toWorldEditNbt(nbt);
                                BaseBlock newBlockWithNbt = weState.toBaseBlock(weNbtTag);
                                editSession.setBlock(vec, newBlockWithNbt);

                                if (mask.updateInstrument) {
                                    BlockState targetState = InstrumentBlockRegistry
                                            .getBlockStateFromInstrument(data.instrument);
                                    if (targetState != null)
                                        editSession.setBlock(vec.add(0, -1, 0), FabricAdapter.adapt(targetState));
                                }
                                count++;
                            }
                        }
                    }
                }
                session.remember(editSession);
            }
            player.sendMessage(Text.of("§a[NewNoteBlock] WorldEdit 操作完成，共更新了 " + count + " 个音符盒。"), false);

        } catch (IncompleteRegionException e) {
            player.sendMessage(Text.of("§c[NewNoteBlock] 选区失效。"), false);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(Text.of("§c[NewNoteBlock] 发生错误: " + e.getMessage()), false);
        }
    }

    private static CompoundTag toWorldEditNbt(NbtCompound nbt) {
        Map<String, Tag> valueMap = new HashMap<>();
        for (String key : nbt.getKeys()) {
            NbtElement element = nbt.get(key);
            Tag convertedTag = convertElement(element);
            if (convertedTag != null)
                valueMap.put(key, convertedTag);
        }
        return new CompoundTag(valueMap);
    }

    private static Tag convertElement(NbtElement element) {
        if (element == null)
            return null;
        if (element instanceof NbtInt)
            return new IntTag(((NbtInt) element).intValue());
        if (element instanceof NbtByte)
            return new ByteTag(((NbtByte) element).byteValue());
        if (element instanceof NbtShort)
            return new ShortTag(((NbtShort) element).shortValue());
        if (element instanceof NbtLong)
            return new LongTag(((NbtLong) element).longValue());
        if (element instanceof NbtFloat)
            return new FloatTag(((NbtFloat) element).floatValue());
        if (element instanceof NbtDouble)
            return new DoubleTag(((NbtDouble) element).doubleValue());
        if (element instanceof NbtString)
            return new StringTag(element.asString());
        if (element instanceof NbtByteArray)
            return new ByteArrayTag(((NbtByteArray) element).getByteArray());
        if (element instanceof NbtIntArray)
            return new IntArrayTag(((NbtIntArray) element).getIntArray());
        if (element instanceof NbtLongArray)
            return new LongArrayTag(((NbtLongArray) element).getLongArray());
        if (element instanceof NbtCompound)
            return toWorldEditNbt((NbtCompound) element);
        if (element instanceof NbtList) {
            NbtList list = (NbtList) element;
            List<Tag> tagList = new ArrayList<>();
            for (NbtElement sub : list) {
                Tag converted = convertElement(sub);
                if (converted != null)
                    tagList.add(converted);
            }
            if (tagList.isEmpty())
                return new ListTag(StringTag.class, new ArrayList<>());
            return new ListTag(tagList.get(0).getClass(), tagList);
        }
        return null;
    }

    private static float applyOpFloat(float current, float val, int op, float min, float max) {
        float result = current;
        switch (op) {
            case 0: // +
                result += val;
                break;
            case 1: // -
                result -= val;
                break;
            case 2: // *
                result *= val;
                break;
            case 3: // /
                if (val != 0)
                    result /= val;
                break;
        }
        return Math.max(min, Math.min(result, max));
    }

    private static String applyOpString(String currentExp, float val, int op) {
        // 0:+, 1:-, 2:*, 3:/
        String operator = "";
        switch (op) {
            case 0:
                operator = "+";
                break;
            case 1:
                operator = "-";
                break;
            case 2:
                operator = "*";
                break;
            case 3:
                operator = "/";
                break;
        }
        return "(" + currentExp + ") " + operator + " " + val;
    }
}