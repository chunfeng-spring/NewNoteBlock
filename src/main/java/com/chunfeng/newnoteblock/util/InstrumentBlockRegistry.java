package com.chunfeng.newnoteblock.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.Instrument;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class InstrumentBlockRegistry {

    // 正向映射：乐器ID -> 方块 (用于 GUI 放置方块)
    private static final Map<String, Block> INSTRUMENT_TO_BLOCK = new HashMap<>();

    // 反向映射：方块 -> 乐器ID (用于识别下方方块)
    private static final Map<Block, String> BLOCK_TO_INSTRUMENT = new HashMap<>();

    // [新增] UI 分类定义
    public static final String[] CATEGORIES = { "原版音符盒音色", "原版音效", "midi", "钢琴", "吉他", "贝斯", "鼓", "弦乐", "颤音琴" };

    // [新增] UI 映射表：分类 -> (显示名称 -> 乐器ID)
    public static final Map<String, LinkedHashMap<String, String>> UI_INSTRUMENT_MAP = new LinkedHashMap<>();

    static {
        // --- 原版映射 (逻辑不变) ---
        register("harp", Blocks.DIRT);
        register("basedrum", Blocks.STONE);
        register("snare", Blocks.SAND);
        register("hat", Blocks.GLASS);
        register("bass", Blocks.OAK_PLANKS);
        register("flute", Blocks.CLAY);
        register("bell", Blocks.GOLD_BLOCK);
        register("guitar", Blocks.LIGHT_BLUE_WOOL);
        register("chime", Blocks.PACKED_ICE);
        register("xylophone", Blocks.BONE_BLOCK);
        register("iron_xylophone", Blocks.IRON_BLOCK);
        register("cow_bell", Blocks.SOUL_SAND);
        register("didgeridoo", Blocks.PUMPKIN);
        register("bit", Blocks.EMERALD_BLOCK);
        register("banjo", Blocks.HAY_BLOCK);
        register("pling", Blocks.GLOWSTONE);

        // 原版音效
        register("anvil", Blocks.ANVIL);
        register("snare2", Blocks.POLISHED_DIORITE);
        register("chain", Blocks.RED_SANDSTONE);
        register("nether_brick", Blocks.NETHER_BRICKS);
        register("brushing", Blocks.RED_SAND);
        register("extinguish_fire", Blocks.WHITE_STAINED_GLASS);
        register("trigger_short", Blocks.OBSIDIAN);
        register("trigger", Blocks.CRYING_OBSIDIAN);
        register("flintandsteel", Blocks.COAL_BLOCK);
        register("firework", Blocks.COAL_ORE);
        register("experience", Blocks.DIAMOND_ORE);
        register("amethyst", Blocks.AMETHYST_BLOCK);

        // MIDI Instruments
        register("0", Blocks.LIGHT_BLUE_CONCRETE); // Acoustic_Grand_Piano
        register("1", Blocks.ORANGE_CONCRETE); // Bright_Acoustic_Piano
        register("2", Blocks.MAGENTA_CONCRETE); // Electric_Grand_Piano
        register("3", Blocks.LIGHT_GRAY_CONCRETE); // Honky-tonk_Piano
        register("4", Blocks.RED_CONCRETE); // Electric_Piano_1
        register("5", Blocks.LIME_CONCRETE); // Electric_Piano_2
        register("6", Blocks.PINK_CONCRETE); // Harpsichord
        register("7", Blocks.GRAY_CONCRETE); // Clavinet
        register("8", Blocks.WHITE_TERRACOTTA); // Celesta
        register("9", Blocks.ORANGE_TERRACOTTA); // Glockenspiel
        register("10", Blocks.MAGENTA_TERRACOTTA); // Music_Box
        register("11", Blocks.LIGHT_BLUE_TERRACOTTA); // Vibraphone
        register("12", Blocks.GRAY_TERRACOTTA); // Marimba
        register("13", Blocks.LIME_TERRACOTTA); // Xylophone
        register("14", Blocks.PINK_TERRACOTTA); // Tubular_Bells
        register("15", Blocks.YELLOW_TERRACOTTA); // Dulcimer
        register("16", Blocks.LIGHT_GRAY_TERRACOTTA); // Drawbar_Organ
        register("17", Blocks.CYAN_TERRACOTTA); // Percussive_Organ
        register("18", Blocks.PURPLE_TERRACOTTA); // Rock_Organ
        register("19", Blocks.BLUE_TERRACOTTA); // Church_Organ
        register("20", Blocks.BROWN_TERRACOTTA); // Reed_Organ
        register("21", Blocks.GREEN_TERRACOTTA); // Accordion
        register("22", Blocks.RED_TERRACOTTA); // Harmonica
        register("23", Blocks.BLACK_TERRACOTTA); // Tango_Accordion
        register("24", Blocks.GREEN_WOOL); // Acoustic_Guitar_nylon
        register("25", Blocks.RED_WOOL); // Acoustic_Guitar_steel
        register("26", Blocks.PURPLE_WOOL); // Electric_Guitar_jazz
        register("27", Blocks.BLUE_WOOL); // Electric_Guitar_clean
        register("28", Blocks.WHITE_WOOL); // Electric_Guitar_muted
        register("29", Blocks.ORANGE_WOOL); // Overdriven_Guitar
        register("30", Blocks.MAGENTA_WOOL); // Distortion_Guitar
        register("31", Blocks.YELLOW_WOOL); // Guitar_harmonics
        register("32", Blocks.JUNGLE_PLANKS); // Acoustic_Bass
        register("33", Blocks.ACACIA_PLANKS); // Electric_Bass_finger
        register("34", Blocks.DARK_OAK_PLANKS); // Electric_Bass_pick
        register("35", Blocks.MANGROVE_PLANKS); // Fretless_Bass
        register("36", Blocks.CHERRY_PLANKS); // Slap_Bass_1
        register("37", Blocks.BAMBOO_PLANKS); // Slap_Bass_2
        register("38", Blocks.CRIMSON_PLANKS); // Synth_Bass_1
        register("39", Blocks.WARPED_PLANKS); // Synth_Bass_2
        register("40", Blocks.STRIPPED_OAK_LOG); // Violin
        register("41", Blocks.STRIPPED_SPRUCE_LOG); // Viola
        register("42", Blocks.STRIPPED_BIRCH_LOG); // Cello
        register("43", Blocks.STRIPPED_JUNGLE_LOG); // Contrabass
        register("44", Blocks.STRIPPED_ACACIA_LOG); // Tremolo_Strings
        register("45", Blocks.STRIPPED_DARK_OAK_LOG); // Pizzicato_Strings
        register("46", Blocks.STRIPPED_MANGROVE_LOG); // Orchestral_Harp
        register("47", Blocks.STRIPPED_CHERRY_LOG); // Timpani
        register("48", Blocks.STRIPPED_BAMBOO_BLOCK); // String_Ensemble_1
        register("49", Blocks.STRIPPED_CRIMSON_STEM); // String_Ensemble_2
        register("50", Blocks.STRIPPED_WARPED_STEM); // SynthStrings_1
        register("51", Blocks.BAMBOO_MOSAIC); // SynthStrings_2
        register("52", Blocks.PRISMARINE); // Choir_Aahs
        register("53", Blocks.PRISMARINE_BRICKS); // Voice_Oohs
        register("54", Blocks.DARK_PRISMARINE); // Synth_Voice
        register("55", Blocks.SEA_LANTERN); // Orchestra_Hit
        register("56", Blocks.WHITE_GLAZED_TERRACOTTA); // Trumpet
        register("57", Blocks.ORANGE_GLAZED_TERRACOTTA); // Trombone
        register("58", Blocks.MAGENTA_GLAZED_TERRACOTTA); // Tuba
        register("59", Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA); // Muted_Trumpet
        register("60", Blocks.YELLOW_GLAZED_TERRACOTTA); // French_Horn
        register("61", Blocks.LIME_GLAZED_TERRACOTTA); // Brass_Section
        register("62", Blocks.PINK_GLAZED_TERRACOTTA); // SynthBrass_1
        register("63", Blocks.GRAY_GLAZED_TERRACOTTA); // SynthBrass_2
        register("64", Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA); // Soprano_Sax
        register("65", Blocks.CYAN_GLAZED_TERRACOTTA); // Alto_Sax
        register("66", Blocks.PURPLE_GLAZED_TERRACOTTA); // Tenor_Sax
        register("67", Blocks.BLUE_GLAZED_TERRACOTTA); // Baritone_Sax
        register("68", Blocks.BROWN_GLAZED_TERRACOTTA); // Oboe
        register("69", Blocks.GREEN_GLAZED_TERRACOTTA); // English_Horn
        register("70", Blocks.RED_GLAZED_TERRACOTTA); // Bassoon
        register("71", Blocks.BLACK_GLAZED_TERRACOTTA); // Clarinet
        register("72", Blocks.GRANITE); // Piccolo
        register("73", Blocks.POLISHED_GRANITE); // Flute
        register("74", Blocks.ANDESITE); // Recorder
        register("75", Blocks.POLISHED_ANDESITE); // Pan_Flute
        register("76", Blocks.DIORITE); // Blown_Bottle
        register("77", Blocks.CALCITE); // Shakuhachi
        register("78", Blocks.TUFF); // Whistle
        register("79", Blocks.DRIPSTONE_BLOCK); // Ocarina
        register("80", Blocks.DEEPSLATE); // Lead_1_square
        register("81", Blocks.COBBLED_DEEPSLATE); // Lead_2_sawtooth
        register("82", Blocks.POLISHED_DEEPSLATE); // Lead_3_calliope
        register("83", Blocks.DEEPSLATE_BRICKS); // Lead_4_chiff
        register("84", Blocks.CRACKED_DEEPSLATE_BRICKS); // Lead_5_charang
        register("85", Blocks.DEEPSLATE_TILES); // Lead_6_voice
        register("86", Blocks.CRACKED_DEEPSLATE_TILES); // Lead_7_fifths
        register("87", Blocks.REINFORCED_DEEPSLATE); // Lead_8_bass_lead
        register("88", Blocks.STONE_BRICKS); // Pad_1_new_age
        register("89", Blocks.CRACKED_STONE_BRICKS); // Pad_2_warm
        register("90", Blocks.CHISELED_STONE_BRICKS); // Pad_3_polysynth
        register("91", Blocks.MOSSY_STONE_BRICKS); // Pad_4_choir
        register("92", Blocks.MOSSY_COBBLESTONE); // Pad_5_bowed
        register("93", Blocks.COBBLESTONE); // Pad_6_metallic
        register("94", Blocks.BRICKS); // Pad_7_halo
        register("95", Blocks.MUD_BRICKS); // Pad_8_sweep
        register("96", Blocks.NETHERRACK); // FX_1_rain
        register("97", Blocks.RED_NETHER_BRICKS); // FX_2_soundtrack
        register("98", Blocks.BASALT); // FX_3_crystal
        register("99", Blocks.POLISHED_BASALT); // FX_4_atmosphere
        register("100", Blocks.SMOOTH_BASALT); // FX_5_brightness
        register("101", Blocks.BLACKSTONE); // FX_6_goblins
        register("102", Blocks.POLISHED_BLACKSTONE); // FX_7_echoes
        register("103", Blocks.POLISHED_BLACKSTONE_BRICKS); // FX_8_sci-fi
        register("104", Blocks.GILDED_BLACKSTONE); // Sitar
        register("105", Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS); // Banjo
        register("106", Blocks.PURPUR_BLOCK); // Shamisen
        register("107", Blocks.PURPUR_PILLAR); // Koto
        register("108", Blocks.END_STONE); // Kalimba
        register("109", Blocks.END_STONE_BRICKS); // Bag_pipe
        register("110", Blocks.ROOTED_DIRT); // Fiddle
        register("111", Blocks.COARSE_DIRT); // Shanai
        register("112", Blocks.OAK_LOG); // Tinkle_Bell
        register("113", Blocks.SPRUCE_LOG); // Agogo
        register("114", Blocks.BIRCH_LOG); // Steel_Drums
        register("115", Blocks.JUNGLE_LOG); // Woodblock
        register("116", Blocks.ACACIA_LOG); // Taiko_Drum
        register("117", Blocks.DARK_OAK_LOG); // Melodic_Tom
        register("118", Blocks.MANGROVE_LOG); // Synth_Drum
        register("119", Blocks.CHERRY_LOG); // Reverse_Cymbal
        register("120", Blocks.COPPER_BLOCK); // Guitar_Fret_Noise
        register("121", Blocks.EXPOSED_COPPER); // Breath_Noise
        register("122", Blocks.WEATHERED_COPPER); // Seashore
        register("123", Blocks.OXIDIZED_COPPER); // Bird_Tweet
        register("124", Blocks.CUT_COPPER); // Telephone_Ring
        register("125", Blocks.EXPOSED_CUT_COPPER); // Helicopter
        register("126", Blocks.WEATHERED_CUT_COPPER); // Applause
        register("127", Blocks.OXIDIZED_CUT_COPPER); // Gunshot
        register("128", Blocks.IRON_ORE); // Percussion

        // 钢琴
        register("noire_piano", Blocks.WHITE_CONCRETE);
        register("noire_piano_mono", Blocks.WHITE_CONCRETE_POWDER);
        register("steinway_piano", Blocks.YELLOW_CONCRETE);

        // 吉他
        register("acoustic_guitar", Blocks.BROWN_WOOL);
        register("clean_electric_guitar", Blocks.CYAN_WOOL);
        register("distortion_electric_guitar", Blocks.BLACK_WOOL);

        // 贝斯
        register("acoustic_bass", Blocks.BIRCH_PLANKS);
        register("electric_bass", Blocks.SPRUCE_PLANKS);

        // 鼓
        register("drum", Blocks.SANDSTONE);

        // 弦乐
        register("string", Blocks.QUARTZ_BLOCK);

        // 颤音琴
        register("vibraphone", Blocks.GOLD_ORE);

        initUIMaps();
    }

    private static void initUIMaps() {
        LinkedHashMap<String, String> vanilla = new LinkedHashMap<>();
        vanilla.put("Harp", "harp");
        vanilla.put("Basedrum", "basedrum");
        vanilla.put("Snare", "snare");
        vanilla.put("Hat", "hat");
        vanilla.put("Bass", "bass");
        vanilla.put("Flute", "flute");
        vanilla.put("Bell", "bell");
        vanilla.put("Guitar", "guitar");
        vanilla.put("Chime", "chime");
        vanilla.put("Xylophone", "xylophone");
        vanilla.put("Iron Xylophone", "iron_xylophone");
        vanilla.put("Cow Bell", "cow_bell");
        vanilla.put("Didgeridoo", "didgeridoo");
        vanilla.put("Bit", "bit");
        vanilla.put("Banjo", "banjo");
        vanilla.put("Pling", "pling");
        UI_INSTRUMENT_MAP.put(CATEGORIES[0], vanilla);

        LinkedHashMap<String, String> sfx = new LinkedHashMap<>();
        sfx.put("anvil", "anvil");
        sfx.put("snare2", "snare2");
        sfx.put("chain", "chain");
        sfx.put("nether_brick", "nether_brick");
        sfx.put("brushing", "brushing");
        sfx.put("extinguish_fire", "extinguish_fire");
        sfx.put("trigger_short", "trigger_short");
        sfx.put("trigger", "trigger");
        sfx.put("flintandsteel", "flintandsteel");
        sfx.put("firework", "firework");
        sfx.put("experience", "experience");
        sfx.put("amethyst", "amethyst");
        UI_INSTRUMENT_MAP.put(CATEGORIES[1], sfx);

        // MIDI Instruments
        LinkedHashMap<String, String> midi = new LinkedHashMap<>();
        midi.put("0 - Acoustic_Grand_Piano", "0");
        midi.put("1 - Bright_Acoustic_Piano", "1");
        midi.put("2 - Electric_Grand_Piano", "2");
        midi.put("3 - Honky-tonk_Piano", "3");
        midi.put("4 - Electric_Piano_1", "4");
        midi.put("5 - Electric_Piano_2", "5");
        midi.put("6 - Harpsichord", "6");
        midi.put("7 - Clavinet", "7");
        midi.put("8 - Celesta", "8");
        midi.put("9 - Glockenspiel", "9");
        midi.put("10 - Music_Box", "10");
        midi.put("11 - Vibraphone", "11");
        midi.put("12 - Marimba", "12");
        midi.put("13 - Xylophone", "13");
        midi.put("14 - Tubular_Bells", "14");
        midi.put("15 - Dulcimer", "15");
        midi.put("16 - Drawbar_Organ", "16");
        midi.put("17 - Percussive_Organ", "17");
        midi.put("18 - Rock_Organ", "18");
        midi.put("19 - Church_Organ", "19");
        midi.put("20 - Reed_Organ", "20");
        midi.put("21 - Accordion", "21");
        midi.put("22 - Harmonica", "22");
        midi.put("23 - Tango_Accordion", "23");
        midi.put("24 - Acoustic_Guitar_nylon", "24");
        midi.put("25 - Acoustic_Guitar_steel", "25");
        midi.put("26 - Electric_Guitar_jazz", "26");
        midi.put("27 - Electric_Guitar_clean", "27");
        midi.put("28 - Electric_Guitar_muted", "28");
        midi.put("29 - Overdriven_Guitar", "29");
        midi.put("30 - Distortion_Guitar", "30");
        midi.put("31 - Guitar_harmonics", "31");
        midi.put("32 - Acoustic_Bass", "32");
        midi.put("33 - Electric_Bass_finger", "33");
        midi.put("34 - Electric_Bass_pick", "34");
        midi.put("35 - Fretless_Bass", "35");
        midi.put("36 - Slap_Bass_1", "36");
        midi.put("37 - Slap_Bass_2", "37");
        midi.put("38 - Synth_Bass_1", "38");
        midi.put("39 - Synth_Bass_2", "39");
        midi.put("40 - Violin", "40");
        midi.put("41 - Viola", "41");
        midi.put("42 - Cello", "42");
        midi.put("43 - Contrabass", "43");
        midi.put("44 - Tremolo_Strings", "44");
        midi.put("45 - Pizzicato_Strings", "45");
        midi.put("46 - Orchestral_Harp", "46");
        midi.put("47 - Timpani", "47");
        midi.put("48 - String_Ensemble_1", "48");
        midi.put("49 - String_Ensemble_2", "49");
        midi.put("50 - SynthStrings_1", "50");
        midi.put("51 - SynthStrings_2", "51");
        midi.put("52 - Choir_Aahs", "52");
        midi.put("53 - Voice_Oohs", "53");
        midi.put("54 - Synth_Voice", "54");
        midi.put("55 - Orchestra_Hit", "55");
        midi.put("56 - Trumpet", "56");
        midi.put("57 - Trombone", "57");
        midi.put("58 - Tuba", "58");
        midi.put("59 - Muted_Trumpet", "59");
        midi.put("60 - French_Horn", "60");
        midi.put("61 - Brass_Section", "61");
        midi.put("62 - SynthBrass_1", "62");
        midi.put("63 - SynthBrass_2", "63");
        midi.put("64 - Soprano_Sax", "64");
        midi.put("65 - Alto_Sax", "65");
        midi.put("66 - Tenor_Sax", "66");
        midi.put("67 - Baritone_Sax", "67");
        midi.put("68 - Oboe", "68");
        midi.put("69 - English_Horn", "69");
        midi.put("70 - Bassoon", "70");
        midi.put("71 - Clarinet", "71");
        midi.put("72 - Piccolo", "72");
        midi.put("73 - Flute", "73");
        midi.put("74 - Recorder", "74");
        midi.put("75 - Pan_Flute", "75");
        midi.put("76 - Blown_Bottle", "76");
        midi.put("77 - Shakuhachi", "77");
        midi.put("78 - Whistle", "78");
        midi.put("79 - Ocarina", "79");
        midi.put("80 - Lead_1_square", "80");
        midi.put("81 - Lead_2_sawtooth", "81");
        midi.put("82 - Lead_3_calliope", "82");
        midi.put("83 - Lead_4_chiff", "83");
        midi.put("84 - Lead_5_charang", "84");
        midi.put("85 - Lead_6_voice", "85");
        midi.put("86 - Lead_7_fifths", "86");
        midi.put("87 - Lead_8_bass_lead", "87");
        midi.put("88 - Pad_1_new_age", "88");
        midi.put("89 - Pad_2_warm", "89");
        midi.put("90 - Pad_3_polysynth", "90");
        midi.put("91 - Pad_4_choir", "91");
        midi.put("92 - Pad_5_bowed", "92");
        midi.put("93 - Pad_6_metallic", "93");
        midi.put("94 - Pad_7_halo", "94");
        midi.put("95 - Pad_8_sweep", "95");
        midi.put("96 - FX_1_rain", "96");
        midi.put("97 - FX_2_soundtrack", "97");
        midi.put("98 - FX_3_crystal", "98");
        midi.put("99 - FX_4_atmosphere", "99");
        midi.put("100 - FX_5_brightness", "100");
        midi.put("101 - FX_6_goblins", "101");
        midi.put("102 - FX_7_echoes", "102");
        midi.put("103 - FX_8_sci-fi", "103");
        midi.put("104 - Sitar", "104");
        midi.put("105 - Banjo", "105");
        midi.put("106 - Shamisen", "106");
        midi.put("107 - Koto", "107");
        midi.put("108 - Kalimba", "108");
        midi.put("109 - Bag_pipe", "109");
        midi.put("110 - Fiddle", "110");
        midi.put("111 - Shanai", "111");
        midi.put("112 - Tinkle_Bell", "112");
        midi.put("113 - Agogo", "113");
        midi.put("114 - Steel_Drums", "114");
        midi.put("115 - Woodblock", "115");
        midi.put("116 - Taiko_Drum", "116");
        midi.put("117 - Melodic_Tom", "117");
        midi.put("118 - Synth_Drum", "118");
        midi.put("119 - Reverse_Cymbal", "119");
        midi.put("120 - Guitar_Fret_Noise", "120");
        midi.put("121 - Breath_Noise", "121");
        midi.put("122 - Seashore", "122");
        midi.put("123 - Bird_Tweet", "123");
        midi.put("124 - Telephone_Ring", "124");
        midi.put("125 - Helicopter", "125");
        midi.put("126 - Applause", "126");
        midi.put("127 - Gunshot", "127");
        midi.put("128 - Percussion", "128");
        UI_INSTRUMENT_MAP.put(CATEGORIES[2], midi);

        LinkedHashMap<String, String> piano = new LinkedHashMap<>();
        piano.put("Noire Piano", "noire_piano");
        piano.put("Noire Piano (单声道)", "noire_piano_mono");
        piano.put("Steinway Piano", "steinway_piano");
        UI_INSTRUMENT_MAP.put(CATEGORIES[3], piano);

        LinkedHashMap<String, String> guitar = new LinkedHashMap<>();
        guitar.put("木吉他", "acoustic_guitar");
        guitar.put("清音电吉他", "clean_electric_guitar");
        guitar.put("失真电吉他", "distortion_electric_guitar");
        UI_INSTRUMENT_MAP.put(CATEGORIES[4], guitar);

        LinkedHashMap<String, String> bass = new LinkedHashMap<>();
        bass.put("木贝斯", "acoustic_bass");
        bass.put("电贝斯", "electric_bass");
        UI_INSTRUMENT_MAP.put(CATEGORIES[5], bass);

        LinkedHashMap<String, String> drum = new LinkedHashMap<>();
        drum.put("架子鼓", "drum");
        UI_INSTRUMENT_MAP.put(CATEGORIES[6], drum);

        LinkedHashMap<String, String> string = new LinkedHashMap<>();
        string.put("弦乐", "string");
        UI_INSTRUMENT_MAP.put(CATEGORIES[7], string);

        LinkedHashMap<String, String> vibraphone = new LinkedHashMap<>();
        vibraphone.put("颤音琴", "vibraphone");
        UI_INSTRUMENT_MAP.put(CATEGORIES[8], vibraphone);
    }

    private static void register(String name, Block block) {
        INSTRUMENT_TO_BLOCK.put(name, block);
        BLOCK_TO_INSTRUMENT.put(block, name);
    }

    public static BlockState getBlockStateFromInstrument(String instrumentId) {
        if (instrumentId == null)
            return Blocks.DIRT.getDefaultState();
        Block block = INSTRUMENT_TO_BLOCK.getOrDefault(instrumentId.toLowerCase(), Blocks.DIRT);
        return block.getDefaultState();
    }

    public static String getInstrumentFromBlockState(BlockState state) {
        Block block = state.getBlock();
        if (BLOCK_TO_INSTRUMENT.containsKey(block)) {
            return BLOCK_TO_INSTRUMENT.get(block);
        }
        Instrument instrument = state.getInstrument();
        return instrument.asString();
    }
}
