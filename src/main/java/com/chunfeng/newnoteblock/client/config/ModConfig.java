package com.chunfeng.newnoteblock.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 模组客户端配置，持久化保存到 config/newnoteblock.json
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("newnoteblock.json");

    // ---- 配置项 ----
    public boolean skipGuiWhenHoldingNoteBlock = false;

    // ---- 单例 ----
    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(get()));
        } catch (IOException e) {
            System.err.println("[NewNoteBlock] 配置保存失败: " + e.getMessage());
        }
    }

    private static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                if (config != null)
                    return config;
            } catch (Exception e) {
                System.err.println("[NewNoteBlock] 配置读取失败，使用默认值: " + e.getMessage());
            }
        }
        return new ModConfig();
    }
}
