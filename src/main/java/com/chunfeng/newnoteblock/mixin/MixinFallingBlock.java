package com.chunfeng.newnoteblock.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlock.class)
public class MixinFallingBlock {

    /**
     * 1. 拦截方块被放置时的下落计划
     * 对应反编译代码第 22 行: onBlockAdded
     */
    @Inject(method = "onBlockAdded", at = @At("HEAD"), cancellable = true)
    private void stopFallingOnAdd(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        // 取消这次调用，不让它安排下落计时器
        ci.cancel();
    }

    /**
     * 2. 终极防御：拦截下落逻辑的执行
     * 对应反编译代码第 35 行: scheduledTick
     * 无论是邻居更新(getStateForNeighborUpdate)还是其他原因触发了 tick，
     * 只要到了真正要"下落"的这一刻，直接取消执行。
     */
    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void stopFallingOnTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        // 直接取消，方块永远不会变成实体掉下去
        ci.cancel();
    }
}