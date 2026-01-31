package com.chunfeng.newnoteblock.mixin;

import com.chunfeng.newnoteblock.client.ui.framework.ImGuiImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Shadow @Final private Window window;

    // 1. 游戏启动完成后，初始化 ImGui
    @Inject(method = "<init>", at = @At("TAIL"))
    private void initImGui(CallbackInfo ci) {
        ImGuiImpl.create(window.getHandle());
    }

    // 2. 每一帧渲染结束前，绘制 ImGui
    // 注入在 swapBuffers 之前，确保画在最上层
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;swapBuffers()V"))
    private void renderImGui(boolean tick, CallbackInfo ci) {
        if (ImGuiImpl.INSTANCE != null) {
            ImGuiImpl.INSTANCE.render();
        }
    }

    // 3. 游戏关闭时，释放资源
    @Inject(method = "close", at = @At("HEAD"))
    private void closeImGui(CallbackInfo ci) {
        if (ImGuiImpl.INSTANCE != null) {
            ImGuiImpl.INSTANCE.dispose();
        }
    }
}