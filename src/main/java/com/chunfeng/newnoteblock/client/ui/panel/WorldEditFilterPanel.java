package com.chunfeng.newnoteblock.client.ui.panel;

import com.chunfeng.newnoteblock.client.ui.data.WorldEditData;
import com.chunfeng.newnoteblock.network.WEPacketHandler;
import com.chunfeng.newnoteblock.util.InstrumentBlockRegistry;
import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import java.util.LinkedHashMap;

public class WorldEditFilterPanel {

    public static void render() {
        renderFilterList();
        ImGui.dummy(0, 5);
        renderAddRuleSection();
    }

    private static void renderFilterList() {
        ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "1. 过滤规则");
        ImGui.textDisabled("链式计算选择的 New Noteblock 集合: A and B or C ...");
        ImGui.separator();

        float listHeight = 300;
        if (ImGui.beginChild("RuleList", 0, listHeight, true)) {
            if (WorldEditData.filterRules.isEmpty()) {
                ImGui.textDisabled("无规则 (选中选区内所有音符盒)");
            } else {
                for (int i = 0; i < WorldEditData.filterRules.size(); i++) {
                    WorldEditData.RuleEntry rule = WorldEditData.filterRules.get(i);
                    ImGui.pushID(i);

                    if (i > 0) {
                        ImGui.textColored(1.0f, 0.8f, 0.0f, 1.0f,
                                rule.logic == WEPacketHandler.FilterRule.Logic.AND ? "AND" : "OR");
                        ImGui.sameLine();
                    }

                    if (ImGui.button("删除")) {
                        WorldEditData.filterRules.remove(i);
                        ImGui.popID();
                        continue;
                    }
                    ImGui.sameLine();

                    if (rule.invert)
                        ImGui.textColored(1.0f, 0.3f, 0.3f, 1.0f, "NOT");
                    else
                        ImGui.textColored(0.3f, 1.0f, 0.3f, 1.0f, "IS");

                    ImGui.sameLine();

                    if (rule.type == WEPacketHandler.FilterRule.Type.INSTRUMENT) {
                        ImGui.text("音色: [" + rule.instrumentDisplayName + "]");
                    } else {
                        ImGui.text("音高: [" + rule.minNote + " ~ " + rule.maxNote + "]");
                    }

                    ImGui.popID();
                }
            }
        }
        ImGui.endChild();
    }

    private static void renderAddRuleSection() {
        ImGui.text("添加新规则:");
        if (ImGui.beginTable("AddRuleTable", 3)) {
            ImGui.tableSetupColumn("Logic", ImGuiTableColumnFlags.WidthFixed, 100);
            ImGui.tableSetupColumn("Type", ImGuiTableColumnFlags.WidthFixed, 120);
            ImGui.tableSetupColumn("Value", ImGuiTableColumnFlags.WidthStretch);

            ImGui.tableNextColumn();
            if (WorldEditData.filterRules.isEmpty()) {
                ImGui.alignTextToFramePadding();
                ImGui.textDisabled("START");
            } else {
                String[] logics = { "AND", "OR" };
                ImGui.pushItemWidth(-1);
                ImGui.combo("##AddLogic", WorldEditData.newRuleLogic, logics);
                ImGui.popItemWidth();
            }

            ImGui.tableNextColumn();
            String[] types = { "音色", "音高" };
            ImGui.pushItemWidth(-1);
            ImGui.combo("##AddType", WorldEditData.newRuleType, types);
            ImGui.popItemWidth();

            ImGui.tableNextColumn();
            float availW = ImGui.getContentRegionAvailX();
            float inputsW = availW - 110;

            if (WorldEditData.newRuleType.get() == 0) { // 音色
                float comboW = (inputsW - 5) / 4.0f;
                ImGui.pushItemWidth(comboW);
                // 使用 Registry
                if (ImGui.combo("##AddCat", WorldEditData.newRuleCatIndex, InstrumentBlockRegistry.CATEGORIES)) {
                    WorldEditData.newRuleInstIndex.set(0);
                }
                ImGui.popItemWidth();

                ImGui.sameLine();
                ImGui.pushItemWidth(comboW);
                updateInstrumentListForBuilder();
                ImGui.popItemWidth();
            } else { // 音高
                float inputW = (inputsW - 25) / 8.0f;
                ImGui.pushItemWidth(inputW);
                if (ImGui.inputInt("##NoteMin", WorldEditData.newRuleNoteMin)) {
                    clampNote(WorldEditData.newRuleNoteMin);
                }
                ImGui.popItemWidth();
                ImGui.sameLine();
                ImGui.text("~");
                ImGui.sameLine();
                ImGui.pushItemWidth(inputW);
                if (ImGui.inputInt("##NoteMax", WorldEditData.newRuleNoteMax)) {
                    clampNote(WorldEditData.newRuleNoteMax);
                }
                ImGui.popItemWidth();
            }

            ImGui.sameLine();
            ImGui.alignTextToFramePadding();
            ImGui.checkbox("求补集", WorldEditData.newRuleInvert);

            ImGui.sameLine();
            if (ImGui.button("添加", 75, 0)) {
                addRule();
            }
            ImGui.endTable();
        }
    }

    private static void clampNote(imgui.type.ImInt val) {
        if (val.get() < 0)
            val.set(0);
        if (val.get() > 127)
            val.set(127);
    }

    private static void updateInstrumentListForBuilder() {
        String cat = InstrumentBlockRegistry.CATEGORIES[WorldEditData.newRuleCatIndex.get()];
        LinkedHashMap<String, String> map = InstrumentBlockRegistry.UI_INSTRUMENT_MAP.get(cat);
        if (map != null) {
            String[] list = map.keySet().toArray(new String[0]);
            ImGui.combo("##AddInst", WorldEditData.newRuleInstIndex, list);
        } else {
            ImGui.text("No Instruments");
        }
    }

    private static void addRule() {
        WEPacketHandler.FilterRule.Logic logic = (WorldEditData.newRuleLogic.get() == 0)
                ? WEPacketHandler.FilterRule.Logic.AND
                : WEPacketHandler.FilterRule.Logic.OR;
        boolean invert = WorldEditData.newRuleInvert.get();

        if (WorldEditData.newRuleType.get() == 0) {
            String cat = InstrumentBlockRegistry.CATEGORIES[WorldEditData.newRuleCatIndex.get()];
            LinkedHashMap<String, String> map = InstrumentBlockRegistry.UI_INSTRUMENT_MAP.get(cat);
            if (map == null)
                return;
            String[] list = map.keySet().toArray(new String[0]);

            if (WorldEditData.newRuleInstIndex.get() < list.length) {
                String displayName = list[WorldEditData.newRuleInstIndex.get()];
                String id = map.get(displayName);
                WorldEditData.filterRules.add(new WorldEditData.RuleEntry(logic, invert, id, displayName));
            }
        } else {
            int min = Math.min(WorldEditData.newRuleNoteMin.get(), WorldEditData.newRuleNoteMax.get());
            int max = Math.max(WorldEditData.newRuleNoteMin.get(), WorldEditData.newRuleNoteMax.get());
            WorldEditData.filterRules.add(new WorldEditData.RuleEntry(logic, invert, min, max));
        }
    }
}