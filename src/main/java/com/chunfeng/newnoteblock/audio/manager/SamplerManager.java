package com.chunfeng.newnoteblock.audio.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SamplerManager implements IdentifiableResourceReloadListener {

    private static final Map<String, TreeMap<Integer, Identifier>> SAMPLE_MAP = new HashMap<>();
    private static final Pattern SAMPLE_PATTERN = Pattern.compile("(.+)\\.([0-9]+)\\.ogg$");
    private static final String GENERATED_PACK_NAME = "NewNoteBlock_soundJson";
    private static final Identifier ID = new Identifier("newnoteblock", "sampler_loader");

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static void init() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SamplerManager());
    }

    public static void forceRefresh() {
        Map<String, TreeMap<Integer, Identifier>> newMap = scanAndWrite();

        if (newMap != null) {
            SAMPLE_MAP.clear();
            SAMPLE_MAP.putAll(newMap);
        }
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public Collection<Identifier> getFabricDependencies() {
        return Collections.emptyList();
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager,
            Profiler prepareProfiler, Profiler applyProfiler,
            Executor prepareExecutor, Executor applyExecutor) {

        return CompletableFuture.supplyAsync(() -> {
            prepareProfiler.startTick();
            prepareProfiler.push("Generating NoteBlock Sounds");
            Map<String, TreeMap<Integer, Identifier>> result = scanAndWrite();
            prepareProfiler.pop();
            prepareProfiler.endTick();
            return result;
        }, prepareExecutor)
                .thenCompose(synchronizer::whenPrepared)
                .thenAcceptAsync(newMap -> {
                    applyProfiler.startTick();
                    applyProfiler.push("Applying NoteBlock Index");
                    if (newMap != null) {
                        SAMPLE_MAP.clear();
                        SAMPLE_MAP.putAll(newMap);
                    }
                    applyProfiler.pop();
                    applyProfiler.endTick();
                }, applyExecutor);
    }

    private static Map<String, TreeMap<Integer, Identifier>> scanAndWrite() {
        ensurePackMeta();
        Map<String, TreeMap<Integer, Identifier>> newMap = new HashMap<>();
        Map<String, List<String>> soundsJsonData = new HashMap<>();

        scanPhysicalPacks(newMap, soundsJsonData);

        if (!soundsJsonData.isEmpty()) {
            writeSoundsJson(soundsJsonData);
        } else {
            cleanOldSoundsJson();
        }
        return newMap;
    }

    private static void ensurePackMeta() {
        Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
        Path packMetaFile = gameDir.resolve("resourcepacks").resolve(GENERATED_PACK_NAME).resolve("pack.mcmeta");

        try {
            if (Files.exists(packMetaFile))
                return;
            Files.createDirectories(packMetaFile.getParent());

            JsonObject packMeta = new JsonObject();
            JsonObject packSection = new JsonObject();
            packSection.addProperty("pack_format", 15);
            packSection.addProperty("description", "Auto-generated sounds for NewNoteBlock");
            packMeta.add("pack", packSection);

            try (FileWriter writer = new FileWriter(packMetaFile.toFile())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(packMeta, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void scanPhysicalPacks(Map<String, TreeMap<Integer, Identifier>> mapOut,
            Map<String, List<String>> jsonOut) {
        Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
        Path resourcePacksDir = gameDir.resolve("resourcepacks");

        if (!Files.exists(resourcePacksDir))
            return;

        try {
            try (Stream<Path> packs = Files.list(resourcePacksDir)) {
                packs.forEach(packPath -> {
                    if (packPath.getFileName().toString().equals(GENERATED_PACK_NAME))
                        return;
                    if (Files.isDirectory(packPath)) {
                        scanDirectory(packPath, mapOut, jsonOut);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void scanDirectory(Path root, Map<String, TreeMap<Integer, Identifier>> mapOut,
            Map<String, List<String>> jsonOut) {
        Path soundsDir = root.resolve("assets/minecraft/sounds");
        if (!Files.exists(soundsDir))
            return;

        try (Stream<Path> files = Files.walk(soundsDir)) {
            files.filter(p -> p.toString().endsWith(".ogg")).forEach(path -> {
                Path relative = soundsDir.relativize(path);
                String relPath = relative.toString().replace("\\", "/");
                String filename = path.getFileName().toString();

                Matcher matcher = SAMPLE_PATTERN.matcher(filename);
                if (matcher.matches()) {
                    String instrument = matcher.group(1);
                    try {
                        int basePitch = Integer.parseInt(matcher.group(2));
                        String cleanPath = relPath.substring(0, relPath.length() - 4);

                        String prefix = "newnoteblock/";
                        String eventName = prefix + cleanPath;
                        if (cleanPath.startsWith(prefix))
                            eventName = cleanPath;

                        Identifier eventId = new Identifier("minecraft", eventName);
                        mapOut.computeIfAbsent(instrument, k -> new TreeMap<>()).put(basePitch, eventId);
                        jsonOut.computeIfAbsent(eventName, k -> new ArrayList<>()).add(cleanPath);

                    } catch (NumberFormatException ignored) {
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeSoundsJson(Map<String, List<String>> soundsData) {
        Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
        Path jsonFile = gameDir.resolve("resourcepacks").resolve(GENERATED_PACK_NAME)
                .resolve("assets/minecraft/sounds.json");

        try {
            Files.createDirectories(jsonFile.getParent());
            Map<String, JsonObject> sortedData = new TreeMap<>();

            for (Map.Entry<String, List<String>> entry : soundsData.entrySet()) {
                JsonObject eventObj = new JsonObject();
                JsonArray soundsArr = new JsonArray();
                for (String path : entry.getValue()) {
                    soundsArr.add(path);
                }
                eventObj.add("sounds", soundsArr);
                sortedData.put(entry.getKey(), eventObj);
            }

            try (FileWriter writer = new FileWriter(jsonFile.toFile())) {
                writer.write("{\n");
                int i = 0;
                int size = sortedData.size();
                for (Map.Entry<String, JsonObject> entry : sortedData.entrySet()) {
                    writer.write("  \"" + entry.getKey() + "\": ");
                    GSON.toJson(entry.getValue(), writer);
                    if (i < size - 1) {
                        writer.write(",\n");
                    } else {
                        writer.write("\n");
                    }
                    i++;
                }
                writer.write("}");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void cleanOldSoundsJson() {
        Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
        Path jsonFile = gameDir.resolve("resourcepacks").resolve(GENERATED_PACK_NAME)
                .resolve("assets/minecraft/sounds.json");
        try {
            Files.deleteIfExists(jsonFile);
        } catch (IOException e) {
        }
    }

    public static class SampleResult {
        public final Identifier soundId;
        public final float pitchMultiplier;

        public SampleResult(Identifier id, float mult) {
            this.soundId = id;
            this.pitchMultiplier = mult;
        }
    }

    /**
     * 获取最接近的音效事件ID
     * 
     * @param instrument 乐器名 (如 "piano")
     * @param targetNote 目标音符
     * @return 包含SoundEvent ID和建议的变调倍率
     */
    public static SampleResult getBestSample(String instrument, int targetNote) {
        // 从 map 中查找外置音色
        TreeMap<Integer, Identifier> pitchMap = SAMPLE_MAP.get(instrument);

        // 如果找不到 (说明可能是原版音色，或者未加载)，返回 null
        if (pitchMap == null || pitchMap.isEmpty())
            return null;

        Integer lowerKey = pitchMap.floorKey(targetNote);
        Integer higherKey = pitchMap.ceilingKey(targetNote);
        int bestBase;
        if (lowerKey == null)
            bestBase = higherKey;
        else if (higherKey == null)
            bestBase = lowerKey;
        else
            bestBase = (targetNote - lowerKey < higherKey - targetNote) ? lowerKey : higherKey;

        Identifier bestId = pitchMap.get(bestBase);
        float pitchMult = (float) Math.pow(2.0, (targetNote - bestBase) / 12.0);
        return new SampleResult(bestId, pitchMult);
    }
}