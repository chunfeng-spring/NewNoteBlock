package com.chunfeng.newnoteblock.block;

import com.chunfeng.newnoteblock.audio.data.FilterDefinition;
import com.chunfeng.newnoteblock.audio.data.ReverbDefinition;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class NewNoteBlockEntity extends BlockEntity {
    private int note = 60;
    private String instrument = "harp";
    private float volume = 1.0f;

    private ArrayList<Integer> volumeCurve = new ArrayList<>();
    private ArrayList<Integer> pitchCurve = new ArrayList<>();

    private int pitchRange = 2;
    private int delay = 0;

    private float reverbSend = 0.0f;
    private float[] reverbParams = ReverbDefinition.getDefault();
    private float[] eqParams = FilterDefinition.getDefault();

    // 运动表达式 (保留用于 GUI 显示和再次编辑)
    private String motionExpX = "0";
    private String motionExpY = "0";
    private String motionExpZ = "0";
    private int motionStartTick = 0;
    private int motionEndTick = 40;
    // [新增] 运动模式：true=相对坐标, false=绝对坐标
    private boolean motionMode = true;

    // [新增] 预计算的轨迹数据 (用于播放性能优化)
    private final List<Vec3d> motionPath = new ArrayList<>();

    @Nullable
    private transient ScheduledFuture<?> scheduledSoundFuture;

    public NewNoteBlockEntity(BlockPos pos, BlockState state) {
        super(NewNoteBlock.NEWNOTEBLOCK_ENTITY_TYPE, pos, state);
    }

    public void setScheduledFuture(@Nullable ScheduledFuture<?> future) {
        cancelScheduledSound();
        this.scheduledSoundFuture = future;
    }

    public void cancelScheduledSound() {
        if (this.scheduledSoundFuture != null && !this.scheduledSoundFuture.isDone()) {
            this.scheduledSoundFuture.cancel(false);
            this.scheduledSoundFuture = null;
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.note = nbt.getInt("Note");
        if (nbt.contains("Instrument"))
            this.instrument = nbt.getString("Instrument");
        if (nbt.contains("Volume"))
            this.volume = nbt.getFloat("Volume");

        this.volumeCurve.clear();
        if (nbt.contains("VolumeCurve")) {
            for (int i : nbt.getIntArray("VolumeCurve"))
                this.volumeCurve.add(i);
        }

        this.pitchCurve.clear();
        if (nbt.contains("PitchCurve")) {
            for (int i : nbt.getIntArray("PitchCurve"))
                this.pitchCurve.add(i);
        }

        this.pitchRange = nbt.contains("PitchRange") ? nbt.getInt("PitchRange") : 2;
        this.delay = nbt.contains("Delay") ? nbt.getInt("Delay") : 0;

        if (nbt.contains("ReverbSend"))
            this.reverbSend = nbt.getFloat("ReverbSend");
        if (nbt.contains("ReverbParamsPacked")) {
            int[] packed = nbt.getIntArray("ReverbParamsPacked");
            if (packed.length == ReverbDefinition.PARAM_COUNT) {
                for (int i = 0; i < packed.length; i++)
                    this.reverbParams[i] = packed[i] / 1000.0f;
            }
        }

        if (nbt.contains("EqParamsPacked")) {
            int[] packed = nbt.getIntArray("EqParamsPacked");
            if (packed.length == FilterDefinition.PARAM_COUNT) {
                for (int i = 0; i < packed.length; i++)
                    this.eqParams[i] = packed[i] / 100.0f;
            }
        }

        // 读取运动表达式
        if (nbt.contains("MotionExpX"))
            this.motionExpX = nbt.getString("MotionExpX");
        if (nbt.contains("MotionExpY"))
            this.motionExpY = nbt.getString("MotionExpY");
        if (nbt.contains("MotionExpZ"))
            this.motionExpZ = nbt.getString("MotionExpZ");
        if (nbt.contains("MotionStartTick"))
            this.motionStartTick = nbt.getInt("MotionStartTick");
        if (nbt.contains("MotionEndTick"))
            this.motionEndTick = nbt.getInt("MotionEndTick");
        if (nbt.contains("MotionMode"))
            this.motionMode = nbt.getBoolean("MotionMode");

        // [新增] 读取预计算轨迹 (IntArray -> List<Vec3d>)
        this.motionPath.clear();
        if (nbt.contains("MotionPathX") && nbt.contains("MotionPathY") && nbt.contains("MotionPathZ")) {
            int[] xArr = nbt.getIntArray("MotionPathX");
            int[] yArr = nbt.getIntArray("MotionPathY");
            int[] zArr = nbt.getIntArray("MotionPathZ");
            int size = Math.min(xArr.length, Math.min(yArr.length, zArr.length));
            for (int i = 0; i < size; i++) {
                this.motionPath.add(new Vec3d(xArr[i] / 1000.0, yArr[i] / 1000.0, zArr[i] / 1000.0));
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("Note", this.note);
        nbt.putString("Instrument", this.instrument);
        nbt.putFloat("Volume", this.volume);
        nbt.putIntArray("VolumeCurve", volumeCurve.stream().mapToInt(i -> i).toArray());
        nbt.putIntArray("PitchCurve", pitchCurve.stream().mapToInt(i -> i).toArray());
        nbt.putInt("PitchRange", pitchRange);
        nbt.putInt("Delay", delay);

        nbt.putFloat("ReverbSend", reverbSend);
        int[] packedReverb = new int[reverbParams.length];
        for (int i = 0; i < reverbParams.length; i++)
            packedReverb[i] = (int) (reverbParams[i] * 1000.0f);
        nbt.putIntArray("ReverbParamsPacked", packedReverb);

        int[] packedEq = new int[eqParams.length];
        for (int i = 0; i < eqParams.length; i++)
            packedEq[i] = (int) (eqParams[i] * 100.0f);
        nbt.putIntArray("EqParamsPacked", packedEq);

        // 写入运动表达式
        nbt.putString("MotionExpX", motionExpX != null ? motionExpX : "0");
        nbt.putString("MotionExpY", motionExpY != null ? motionExpY : "0");
        nbt.putString("MotionExpZ", motionExpZ != null ? motionExpZ : "0");
        nbt.putInt("MotionStartTick", motionStartTick);
        nbt.putInt("MotionEndTick", motionEndTick);
        nbt.putBoolean("MotionMode", motionMode);

        // [新增] 写入预计算轨迹 (List<Vec3d> -> IntArray)
        int size = motionPath.size();
        int[] xArr = new int[size];
        int[] yArr = new int[size];
        int[] zArr = new int[size];
        for (int i = 0; i < size; i++) {
            Vec3d v = motionPath.get(i);
            xArr[i] = (int) (v.x * 1000.0);
            yArr[i] = (int) (v.y * 1000.0);
            zArr[i] = (int) (v.z * 1000.0);
        }
        nbt.putIntArray("MotionPathX", xArr);
        nbt.putIntArray("MotionPathY", yArr);
        nbt.putIntArray("MotionPathZ", zArr);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound n = new NbtCompound();
        writeNbt(n);
        return n;
    }

    // [修改] 接收预计算的 path 数据
    public void updateData(int note, String inst, float vol, List<Integer> volCurve, List<Integer> pitch, int pRange,
            int delay,
            float send, float[] rParams, float[] eParams,
            String mExpX, String mExpY, String mExpZ, int mStart, int mEnd, boolean mMode,
            List<Vec3d> pathData) { // [新增参数]
        this.note = note;
        this.instrument = inst;
        this.volume = vol;
        this.volumeCurve = new ArrayList<>(volCurve);
        this.pitchCurve = new ArrayList<>(pitch);
        this.pitchRange = pRange;
        this.delay = Math.max(0, Math.min(5000, delay));

        this.reverbSend = send;
        if (rParams != null && rParams.length == ReverbDefinition.PARAM_COUNT) {
            System.arraycopy(rParams, 0, this.reverbParams, 0, rParams.length);
        }
        if (eParams != null && eParams.length == FilterDefinition.PARAM_COUNT) {
            System.arraycopy(eParams, 0, this.eqParams, 0, eParams.length);
        }

        // 更新运动参数和轨迹数据
        this.motionExpX = mExpX;
        this.motionExpY = mExpY;
        this.motionExpZ = mExpZ;
        this.motionStartTick = mStart;
        this.motionEndTick = mEnd;
        this.motionMode = mMode;

        this.motionPath.clear();
        if (pathData != null) {
            this.motionPath.addAll(pathData);
        }

        // Update note property in BlockState
        if (world != null && !world.isClient) {
            BlockState currentState = getCachedState();
            if (currentState.contains(NewNoteBlock.NewNoteBlockBlock.NOTE)) {
                int noteVal = (Math.floorMod(this.note, 12)) + 1;
                if (currentState.get(NewNoteBlock.NewNoteBlockBlock.NOTE) != noteVal) {
                    world.setBlockState(pos, currentState.with(NewNoteBlock.NewNoteBlockBlock.NOTE, noteVal), 3);
                }
            }
        }

        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public int getNote() {
        return note;
    }

    public String getInstrument() {
        return instrument;
    }

    public float getVolume() {
        return volume;
    }

    public ArrayList<Integer> getVolumeCurve() {
        return volumeCurve;
    }

    public ArrayList<Integer> getPitchCurve() {
        return pitchCurve;
    }

    public int getPitchRange() {
        return pitchRange;
    }

    public int getDelay() {
        return delay;
    }

    public float getReverbSend() {
        return reverbSend;
    }

    public float[] getReverbParams() {
        return reverbParams;
    }

    public float[] getEqParams() {
        return eqParams;
    }

    public String getMotionExpX() {
        return motionExpX;
    }

    public String getMotionExpY() {
        return motionExpY;
    }

    public String getMotionExpZ() {
        return motionExpZ;
    }

    public int getMotionStartTick() {
        return motionStartTick;
    }

    public int getMotionEndTick() {
        return motionEndTick;
    }

    public boolean getMotionMode() {
        return motionMode;
    }

    // [新增]
    public List<Vec3d> getMotionPath() {
        return motionPath;
    }

    @Override
    public void markRemoved() {
        cancelScheduledSound();
        super.markRemoved();
    }
}