package com.chunfeng.newnoteblock.util;

import com.sk89q.worldedit.entity.Player;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import java.lang.reflect.Constructor;

public class WEReflection {
    public static Player getPlayer(ServerPlayerEntity player) {
        try {
            Class<?> clazz = Class.forName("com.sk89q.worldedit.fabric.FabricPlayer");
            Constructor<?> c = clazz.getDeclaredConstructor(ServerPlayerEntity.class);
            c.setAccessible(true);
            return (Player) c.newInstance(player);
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public static com.sk89q.worldedit.world.World getWorld(ServerWorld world) {
        try {
            Class<?> clazz = Class.forName("com.sk89q.worldedit.fabric.FabricWorld");
            Constructor<?> c = clazz.getDeclaredConstructor(net.minecraft.world.World.class);
            c.setAccessible(true);
            return (com.sk89q.worldedit.world.World) c.newInstance(world);
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}