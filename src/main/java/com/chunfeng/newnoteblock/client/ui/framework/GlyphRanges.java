package com.chunfeng.newnoteblock.client.ui.framework;

/**
 * 自定义字体字形范围生成器
 * 包含了 ASCII + 常用标点 + 全量中文
 */
public final class GlyphRanges {

    private GlyphRanges() {
    }

    // ============================================================
    // 终极全能范围 (ASCII + 中文)
    // ============================================================
    // 这个数组直接包含了 ImGui 需要的所有字符范围。
    // 格式：Start, End, Start, End, ..., 0
    private static final short[] ALL_RANGES = {
            // --- 1. 基础英文 (ASCII) ---
            0x0020, 0x00FF, // Basic Latin + Latin Supplement (包含了 C, 4, +, - 等)

            // --- 2. 通用标点 ---
            0x2000, 0x206F, // General Punctuation

            // --- 3. CJK 符号 (句号、引号等) ---
            0x3000, 0x30FF, // CJK Symbols and Punctuation

            // --- 4. 半角/全角形式 ---
            (short) 0xFF00, (short) 0xFFEF,

            // --- 5. 简体中文 (拆分范围以适应 Java short) ---
            // 范围: 0x4E00 - 0x9FAF (约2万汉字)
            0x4E00, 0x7FFF, // Part 1: 正数区
            (short) 0x8000, (short) 0x9FAF, // Part 2: 负数区

            // --- 6. 结束标志 ---
            0
    };

    /**
     * 获取包含 英文+中文 的完整字符范围数组
     * 直接传给 config.setGlyphRanges() 即可，无需 Builder。
     */
    public static short[] getAllRanges() {
        return ALL_RANGES;
    }
}