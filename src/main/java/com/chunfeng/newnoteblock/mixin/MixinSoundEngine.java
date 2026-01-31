package com.chunfeng.newnoteblock.mixin;

import net.minecraft.client.sound.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {

    /**
     * [修复] 强制修改流式音源池的容量。
     * * 原理分析：
     * 1. Minecraft 计算出的流式通道数 (变量 j) 受到 sqrt(总通道数) 的限制，最大通常只有 16。
     * 2. 在 SoundEngine.init 中，流式池是**第二个**被初始化的 SourceSetImpl (对应变量
     * this.staticSources)。
     * (注意：反编译代码中变量名 staticSources 和 streamingSources 是反的，不要被名字迷惑，看 createSource
     * 逻辑可知)
     * * 解决方案：
     * 直接拦截 SourceSetImpl 构造函数的参数，强制将其改为 128。
     */
    @ModifyArg(method = "init", at = @At(value = "INVOKE",
            // 目标：new SourceSetImpl(int maxSourceCount)
            target = "Lnet/minecraft/client/sound/SoundEngine$SourceSetImpl;<init>(I)V", ordinal = 1 // 重点：拦截第 2 次调用（第 1
                                                                                                     // 次是普通音效池，第 2
                                                                                                     // 次才是流式池）
    ))
    private int forceIncreaseStreamingLimit(int originalSize) {
        // [调试] 你可以在这里打印一下 originalSize，你会发现它之前只有 8 或 16
        // System.out.println("NewNoteBlock: Forcing streaming pool size from " +
        // originalSize + " to 128");

        // 返回一个足够大的值，128 足以应对极高频的红石音乐
        // 只要不超过硬件总上限 (通常256)，就是安全的
        return 256;
    }
}