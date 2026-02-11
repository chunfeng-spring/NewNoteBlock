package com.chunfeng.newnoteblock.block;

import com.chunfeng.newnoteblock.NewNoteBlockMod;
import com.chunfeng.newnoteblock.client.ui.screen.NewNoteBlockScreen;
import com.chunfeng.newnoteblock.audio.manager.ServerSoundManager;
import com.chunfeng.newnoteblock.util.InstrumentBlockRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NewNoteBlock {
    public static BlockEntityType<NewNoteBlockEntity> NEWNOTEBLOCK_ENTITY_TYPE;

    public static Block register(String id, Block block) {
        registerBlockItems(id, block);
        return Registry.register(Registries.BLOCK, new Identifier(NewNoteBlockMod.MOD_ID, id), block);
    }

    public static void registerBlockItems(String id, Block block) {
        Registry.register(Registries.ITEM, new Identifier(NewNoteBlockMod.MOD_ID, id),
                new BlockItem(block, new Item.Settings()));
    }

    private static void addItemToItemGroup(FabricItemGroupEntries entries) {
        entries.add(NEWNOTEBLOCK);
    }

    public static class NewNoteBlockBlock extends Block implements BlockEntityProvider {
        public static final BooleanProperty POWERED = Properties.POWERED;
        public static final IntProperty NOTE = IntProperty.of("note", 1, 12);

        private static final ScheduledExecutorService scheduler = Executors
                .newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "NoteBlockDelayScheduler");
                    thread.setDaemon(true);
                    return thread;
                });

        public NewNoteBlockBlock(Settings settings) {
            super(settings);
            this.setDefaultState(this.stateManager.getDefaultState().with(POWERED, false).with(NOTE, 1));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
            builder.add(POWERED, NOTE);
        }

        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
            return new NewNoteBlockEntity(pos, state);
        }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                BlockHitResult hit) {
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }

            // [新增] 如果玩家手持木斧（WorldEdit 选区工具），不打开 GUI，让 WorldEdit 处理
            net.minecraft.item.ItemStack heldItem = player.getStackInHand(hand);
            if (heldItem.isOf(net.minecraft.item.Items.WOODEN_AXE)) {
                return ActionResult.PASS;
            }

            // [新增] 如果玩家手持新音符盒，不打开 GUI，方便放置方块
            if (heldItem.isOf(NEWNOTEBLOCK.asItem())) {
                return ActionResult.PASS;
            }

            if (world.isClient) {
                // [修复] 先发送同步请求到服务端，等服务端返回数据后再打开 GUI
                // 这解决了 WorldEdit 选区导致客户端 BlockEntity 数据不同步的问题
                com.chunfeng.newnoteblock.network.NotePacketHandler.requestSync(pos);

                MinecraftClient.getInstance().setScreen(new Screen(Text.of("Note Block Editor")) {
                    @Override
                    protected void init() {
                        // 不再在这里直接读取客户端 BlockEntity 数据
                        // 数据会通过网络包从服务端同步过来，并由 openWithSyncedData 处理
                    }

                    @Override
                    public void close() {
                        // 手动调用 ImGui 的关闭逻辑
                        NewNoteBlockScreen.close();
                        super.close();
                    }

                    @Override
                    public boolean shouldPause() {
                        return false;
                    }

                    @Override
                    public void removed() {
                        NewNoteBlockScreen.close();
                        super.removed();
                    }
                });
            }

            return ActionResult.SUCCESS;
        }

        @Override
        public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
            if (!world.isClient) {
                triggerNote((ServerWorld) world, pos);
            }
            super.onBlockBreakStart(state, world, pos, player);
        }

        @Override
        public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos,
                boolean notify) {
            if (world.isClient)
                return;

            boolean isPowered = world.isReceivingRedstonePower(pos);
            boolean wasPowered = state.get(POWERED);

            if (isPowered != wasPowered) {
                if (isPowered) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof NewNoteBlockEntity noteBe) {
                        int delay = noteBe.getDelay();
                        if (delay > 0) {
                            ScheduledFuture<?> future = scheduler.schedule(() -> {
                                if (world instanceof ServerWorld serverWorld) {
                                    serverWorld.getServer().execute(() -> {
                                        if (world.getBlockState(pos).isOf(this)) {
                                            triggerNote(serverWorld, pos);
                                        }
                                    });
                                }
                            }, delay, TimeUnit.MILLISECONDS);
                            noteBe.setScheduledFuture(future);
                        } else {
                            triggerNote((ServerWorld) world, pos);
                        }
                    } else {
                        triggerNote((ServerWorld) world, pos);
                    }
                }
                world.setBlockState(pos, state.with(POWERED, isPowered), 3);
            }

            if (sourcePos.equals(pos.down())) {
                BlockState downState = world.getBlockState(pos.down());
                String newInstrument = InstrumentBlockRegistry.getInstrumentFromBlockState(downState);

                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof NewNoteBlockEntity noteBe) {
                    if (!newInstrument.equals(noteBe.getInstrument())) {
                        noteBe.updateData(
                                noteBe.getNote(),
                                newInstrument,
                                noteBe.getVolume(), // [Modified] Pass volume
                                noteBe.getVolumeCurve(),
                                noteBe.getPitchCurve(),
                                noteBe.getPitchRange(),
                                noteBe.getDelay(),
                                noteBe.getReverbSend(),
                                noteBe.getReverbParams(),
                                noteBe.getEqParams(),
                                // [修改] 补全运动参数
                                noteBe.getMotionExpX(),
                                noteBe.getMotionExpY(),
                                noteBe.getMotionExpZ(),
                                noteBe.getMotionStartTick(),
                                noteBe.getMotionEndTick(),
                                noteBe.getMotionMode(), // [新增]
                                noteBe.getMotionPath());
                    }
                }
            }
        }

        private void triggerNote(ServerWorld world, BlockPos pos) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof NewNoteBlockEntity noteBe) {
                String inst = noteBe.getInstrument();
                if (inst == null)
                    inst = "harp";

                ServerSoundManager.playSound(
                        world,
                        pos,
                        inst,
                        noteBe.getNote(),
                        noteBe.getVolume(), // [Modified] Pass volume
                        noteBe.getVolumeCurve(),
                        noteBe.getPitchCurve(),
                        noteBe.getPitchRange(),
                        noteBe.getReverbSend(),
                        noteBe.getReverbParams(),
                        noteBe.getEqParams(),
                        // [修改] 传递运动参数
                        noteBe.getMotionStartTick(),
                        noteBe.getMotionEndTick(),
                        noteBe.getMotionMode(), // [新增]
                        noteBe.getMotionPath());

                double particleColor = (noteBe.getNote() % 25) / 24.0D;
                world.spawnParticles(
                        ParticleTypes.NOTE,
                        pos.getX() + 0.5D,
                        pos.getY() + 1.2D,
                        pos.getZ() + 0.5D,
                        0,
                        particleColor,
                        0.0D,
                        0.0D,
                        1.0D);
            }
        }

        @Override
        public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                ItemStack itemStack) {
            super.onPlaced(world, pos, state, placer, itemStack);
            if (!world.isClient) {
                BlockState downState = world.getBlockState(pos.down());
                String instrument = InstrumentBlockRegistry.getInstrumentFromBlockState(downState);

                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof NewNoteBlockEntity noteBe) {
                    noteBe.updateData(
                            noteBe.getNote(),
                            instrument,
                            noteBe.getVolume(), // [Modified] Pass volume
                            noteBe.getVolumeCurve(),
                            noteBe.getPitchCurve(),
                            noteBe.getPitchRange(),
                            noteBe.getDelay(),
                            noteBe.getReverbSend(),
                            noteBe.getReverbParams(),
                            noteBe.getEqParams(),
                            // [修改] 补全运动参数
                            noteBe.getMotionExpX(),
                            noteBe.getMotionExpY(),
                            noteBe.getMotionExpZ(),
                            noteBe.getMotionStartTick(),
                            noteBe.getMotionEndTick(),
                            noteBe.getMotionMode(), // [新增]
                            noteBe.getMotionPath());
                }
            }
        }

        @Override
        public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
            if (!state.isOf(newState.getBlock())) {
                if (!world.isClient) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof NewNoteBlockEntity noteBe) {
                        noteBe.cancelScheduledSound();
                    }
                }
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    public static final Block NEWNOTEBLOCK = register("newnoteblock",
            new NewNoteBlockBlock(FabricBlockSettings.copyOf(Blocks.NOTE_BLOCK)
                    .strength(4.0f)
                    .luminance(state -> state.get(NewNoteBlockBlock.POWERED) ? 15 : 0)));

    public static void registerBlock() {
        NEWNOTEBLOCK_ENTITY_TYPE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(NewNoteBlockMod.MOD_ID, "newnoteblock_entity"),
                FabricBlockEntityTypeBuilder.create(NewNoteBlockEntity::new, NEWNOTEBLOCK).build());
    }

    public static void registerItems() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(NewNoteBlock::addItemToItemGroup);
    }
}