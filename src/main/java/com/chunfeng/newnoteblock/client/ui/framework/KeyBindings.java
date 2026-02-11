package com.chunfeng.newnoteblock.client.ui.framework;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    // [修改] 变量名 openWEGuiKey
    public static KeyBinding openWEGuiKey;

    public static void register() {
        // [修改] 翻译键名 key.newnoteblock.open_we_gui
        openWEGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.newnoteblock.open_we_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category.newnoteblock.title"));
    }
}
