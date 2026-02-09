package com.chunfeng.newnoteblock.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Replay Mod 兼容性工具类
 * 使用反射安全地检测是否正在回放，避免硬依赖
 */
public class ReplayModCompat {
    private static Boolean replayModPresent = null;
    private static Class<?> replayModReplayClass = null;
    private static Field instanceField = null;
    private static Method getReplayHandlerMethod = null;

    /**
     * 检测当前是否处于 Replay Mod 的回放模式
     * 
     * @return true 表示正在回放中，应跳过 GUI 打开；false 表示正常游戏
     */
    public static boolean isInReplay() {
        // 1. 检查 Replay Mod 是否安装
        if (replayModPresent == null) {
            try {
                replayModReplayClass = Class.forName("com.replaymod.replay.ReplayModReplay");
                instanceField = replayModReplayClass.getField("instance");
                getReplayHandlerMethod = replayModReplayClass.getMethod("getReplayHandler");
                replayModPresent = true;
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
                replayModPresent = false;
            }
        }

        if (!replayModPresent) {
            return false;
        }

        // 2. 通过反射检查回放状态
        try {
            Object instance = instanceField.get(null);
            if (instance == null) {
                return false;
            }

            // 调用 getReplayHandler() 方法
            Object handler = getReplayHandlerMethod.invoke(instance);

            // 如果 handler 不为 null，表示正在回放
            return handler != null;
        } catch (Exception e) {
            // 反射失败时，保守地返回 false（不阻止 GUI 打开）
            return false;
        }
    }
}
