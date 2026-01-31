package com.chunfeng.newnoteblock.mixin;

import com.chunfeng.newnoteblock.audio.data.FilterDefinition;
import com.chunfeng.newnoteblock.audio.dsp.ProcessingAudioStream;
import com.chunfeng.newnoteblock.audio.engine.NoteBlockAudioEngine;
import com.chunfeng.newnoteblock.audio.manager.ClientSoundManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.sound.*;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(SoundSystem.class)
public class MixinSoundSystem {

    @Shadow @Final private Map<SoundInstance, Channel.SourceManager> sources;

    // [上下文] 用于在 play 方法内部传递当前正在处理的 Mod 音效实例
    @Unique
    private final ThreadLocal<ClientSoundManager.ModSoundInstance> currentModSound = new ThreadLocal<>();

    // ==========================================================================
    // 1. 旧功能迁移 (解除音调限制 & 修改衰减距离)
    // ==========================================================================

    /**
     * [旧功能] 解除原本 0.5 ~ 2.0 的音调(Pitch)限制
     * 允许极低或极高的音调播放
     */
    @Redirect(
            method = "getAdjustedPitch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;clamp(FFF)F"
            )
    )
    private float removePitchClamp(float value, float min, float max) {
        return value; // 直接返回原值，不进行 Clamp 限制
    }

    /**
     * [旧功能] 修改线性衰减类型的声音传播距离
     * 强制将线性衰减距离改为 48.0F (原本可能较短)
     */
    @ModifyVariable(
            method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = @At(value = "STORE"),
            ordinal = 1 // 修改局部变量 g (attenuation distance)
    )
    private float modifyAttenuationDistance(float g, SoundInstance soundInstance) {
        if (soundInstance.getAttenuationType() == SoundInstance.AttenuationType.LINEAR) {
            return 48.0F;
        }
        return g;
    }

    // ==========================================================================
    // 2. 新功能区域 (DSP 拦截 & 混响/EQ 应用)
    // ==========================================================================

    /**
     * [步骤 A] 在 play 方法开始时，捕获当前的声音实例。
     * 如果它是我们模组的声音(ModSoundInstance)，则存入 ThreadLocal 供后续使用。
     */
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void captureSoundContext(SoundInstance sound, CallbackInfo ci) {
        if (sound instanceof ClientSoundManager.ModSoundInstance modSound) {
            currentModSound.set(modSound);
        } else {
            currentModSound.remove();
        }
    }

    /**
     * [步骤 B - 关键修复] 强制将我们的模组声音识别为"流式(Streamed)"。
     * 只有流式声音才会走 loadStreamed 方法，从而被我们的 EQ 处理器拦截。
     * 如果不加这个，短声音会走 loadStatic，导致 EQ 失效。
     */
    @Redirect(
            method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/Sound;isStreamed()Z"
            )
    )
    private boolean forceStreamForModSounds(Sound sound) {
        ClientSoundManager.ModSoundInstance modSound = currentModSound.get();

        // [优化核心]
        // 只有当模组声音存在，并且 EQ 参数【不是】默认值时，才强制开启流式模式！
        // 混响(Reverb)不需要流式模式，它是 OpenAL 硬件效果，静态音效也能用。
        if (modSound != null && !FilterDefinition.isNeutral(modSound.eqParams)) {
            return true;
        }
        // 否则返回原始设置
        return sound.isStreamed();
    }

    /**
     * [步骤 C] 拦截音频流加载 (使用 WrapOperation 解决与其他模组的 Redirect 冲突)。
     * 这里我们获取原始音频流，并将其包裹在 ProcessingAudioStream 中以应用 EQ。
     */
    @WrapOperation(
            method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = @At(
                    value = "INVOKE",
                    // 目标方法名必须是 loadStreamed (对应反编译代码)
                    target = "Lnet/minecraft/client/sound/SoundLoader;loadStreamed(Lnet/minecraft/util/Identifier;Z)Ljava/util/concurrent/CompletableFuture;"
            )
    )
    private CompletableFuture<AudioStream> wrapLoadStreamed(SoundLoader instance, Identifier id, boolean repeatInstantly, Operation<CompletableFuture<AudioStream>> original) {
        // 调用原始逻辑 (或其他模组的重定向逻辑) 获取流
        CompletableFuture<AudioStream> originalFuture = original.call(instance, id, repeatInstantly);

        // 获取上下文中的 Mod 音效
        ClientSoundManager.ModSoundInstance modSound = currentModSound.get();

        // 如果是我们模组的声音，且包含 EQ 参数，则进行包装
        if (modSound != null) {

            // [优化] 如果 EQ 参数是默认值/无效值，直接返回原始流，跳过 DSP 包装
            if (FilterDefinition.isNeutral(modSound.eqParams)) {
                return originalFuture;
            }

            return originalFuture.thenApply(originalStream -> {
                // 将原始流包装进 DSP 处理流 (应用 EQ)
                return new ProcessingAudioStream(originalStream, modSound.eqParams);
            });
        }

        return originalFuture;
    }

    /**
     * [步骤 D] 清理 ThreadLocal，防止内存泄漏或逻辑污染。
     */
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("RETURN"))
    private void clearSoundContext(SoundInstance sound, CallbackInfo ci) {
        currentModSound.remove();
    }

    /**
     * [步骤 E] 在声音开始播放后 (TAIL)，应用 OpenAL 混响参数。
     * 此时 Source 已经创建完毕，我们可以获取 ID 并设置 EFX 效果。
     */
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("TAIL"))
    private void onPlay(SoundInstance sound, CallbackInfo ci) {
        if (sound instanceof ClientSoundManager.ModSoundInstance modSound) {
            Channel.SourceManager sourceManager = this.sources.get(sound);
            if (sourceManager != null) {
                // 在 SoundEngine 线程上执行
                sourceManager.run(source -> {
                    // 获取 OpenAL Source ID
                    int sourceId = ((SourceAccessor) source).getPointer();

                    // 应用混响参数 (filter 参数已移除，现在只负责 Reverb)
                    NoteBlockAudioEngine.ReverbTask task = new NoteBlockAudioEngine.ReverbTask(
                            sourceId,
                            modSound.reverbParams,
                            modSound.reverbSend
                    );
                    NoteBlockAudioEngine.processTask(task);
                });
            }
        }
    }

    /**
     * [步骤 F] 当声音停止时，断开混响连接，释放资源。
     */
    @Inject(method = "stop(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void onStop(SoundInstance sound, CallbackInfo ci) {
        if (sound instanceof ClientSoundManager.ModSoundInstance) {
            Channel.SourceManager sourceManager = this.sources.get(sound);
            if (sourceManager != null) {
                sourceManager.run(source -> {
                    int sourceId = ((SourceAccessor) source).getPointer();
                    NoteBlockAudioEngine.disconnect(sourceId);
                });
            }
        }
    }
}