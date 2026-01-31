package com.chunfeng.newnoteblock.client.ui.panel;

import com.chunfeng.newnoteblock.client.ui.data.NoteData;
import com.chunfeng.newnoteblock.client.ui.screen.NewNoteBlockScreen;
import com.chunfeng.newnoteblock.client.ui.widget.GraphWidget;
import com.chunfeng.newnoteblock.util.InstrumentBlockRegistry;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiTableRowFlags;
import imgui.flag.ImGuiWindowFlags;

import java.util.LinkedHashMap;

public class NoteSettingsPanel {

    /**
     * @param data         当前编辑的数据对象 (来自 NewNoteBlockScreen 或 WorldEditData)
     * @param onDataChange 数据变更回调 (用于发送网络包)
     */
    public static void render(float width, float height, NoteData data, Runnable onDataChange) {
        boolean visible = ImGui.beginChild("TopSection", width, height, false,
                ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        if (visible) {
            if (ImGui.beginTable("TopLayout", 2, ImGuiTableFlags.Resizable)) {
                ImGui.tableSetupColumn("Graphs", ImGuiTableColumnFlags.WidthStretch, 0.65f);
                ImGui.tableSetupColumn("Selectors", ImGuiTableColumnFlags.WidthStretch, 0.35f);

                ImGui.tableNextColumn();
                renderGraphs(ImGui.getContentRegionAvailY(), data, onDataChange);

                ImGui.tableNextColumn();
                renderControls(data, onDataChange);

                ImGui.endTable();
            }
        }
        ImGui.endChild();
    }

    private static void renderGraphs(float totalHeight, NoteData data, Runnable onDataChange) {
        float leftColH = totalHeight;
        String rangeLabel = "弯音范围(半音): ";
        String tickLabel = "时长(Tick): ";
        ImVec2 labelSizeRange = new ImVec2();
        ImGui.calcTextSize(labelSizeRange, rangeLabel);
        ImVec2 labelSizeTick = new ImVec2();
        ImGui.calcTextSize(labelSizeTick, tickLabel);
        float btnRowH = labelSizeRange.y + 10.0f;
        float textH = ImGui.getTextLineHeightWithSpacing();
        float tickH = 20.0f;
        float sectionTotalH = (leftColH - btnRowH) / 2.0f;
        float rawGraphRectH = sectionTotalH - textH - tickH;
        float finalGraphH = rawGraphRectH * 0.8f;

        if (ImGui.beginTable("GraphVerticalTable", 1)) {
            ImGui.tableNextRow(ImGuiTableRowFlags.None, sectionTotalH);
            ImGui.tableSetColumnIndex(0);
            float cellW = ImGui.getContentRegionAvailX();

            ImGui.text("音量线图：");
            GraphWidget.render(cellW, finalGraphH, data.volumeCurve, 0xFF00AAFF, "Vol", data.pitchRange.get(), 0,
                    onDataChange);

            ImGui.tableNextRow(ImGuiTableRowFlags.None, btnRowH);
            ImGui.tableSetColumnIndex(0);

            float gap = 20.0f;
            float inputW = 80.0f;
            float sliderW = 300.0f;
            float volInputW = 120.0f; // Width for "Master Volume" input

            // Calculate total width to center align
            // Order: Volume Input + gap + Tick Label + Slider + gap + Pitch Range Label +
            // Input
            String volLabel = "主音量: ";
            ImVec2 labelSizeVol = new ImVec2();
            ImGui.calcTextSize(labelSizeVol, volLabel);

            float totalGroupW = labelSizeVol.x + volInputW + gap + labelSizeTick.x + sliderW + gap + labelSizeRange.x
                    + inputW;
            float startX = ImGui.getCursorPosX() + (cellW - totalGroupW) / 2.0f;

            ImGui.setCursorPosX(startX);

            // 1. Master Volume Control
            ImGui.alignTextToFramePadding();
            ImGui.text(volLabel);
            ImGui.sameLine();
            ImGui.pushItemWidth(volInputW);
            float[] volVal = { data.volume.get() };
            if (ImGui.dragFloat("##MasterVolume", volVal, 0.01f, 0.0f, 1.0f, "%.2f")) {
                data.volume.set(Math.max(0.0f, Math.min(1.0f, volVal[0])));
                if (onDataChange != null)
                    onDataChange.run();
            }
            ImGui.popItemWidth();

            ImGui.sameLine(0, gap);

            // 2. Tick Count Slider
            ImGui.alignTextToFramePadding();
            ImGui.text(tickLabel);
            ImGui.sameLine();
            ImGui.pushItemWidth(sliderW);

            if (ImGui.sliderInt("##TickCount", data.tickCount.getData(), 0, 100)) {
                int target = data.tickCount.get();
                if (target == 0) {
                    data.volumeCurve.clear();
                    data.pitchCurve.clear();
                } else {
                    while (data.volumeCurve.size() < target) {
                        int lastVol = data.volumeCurve.isEmpty() ? 100
                                : data.volumeCurve.get(data.volumeCurve.size() - 1);
                        data.volumeCurve.add(lastVol);
                        data.pitchCurve.add(0);
                    }
                    while (data.volumeCurve.size() > target) {
                        data.volumeCurve.remove(data.volumeCurve.size() - 1);
                        if (!data.pitchCurve.isEmpty())
                            data.pitchCurve.remove(data.pitchCurve.size() - 1);
                    }
                }
                if (onDataChange != null)
                    onDataChange.run();
            }
            ImGui.popItemWidth();

            ImGui.sameLine(0, gap);
            ImGui.text(rangeLabel);
            ImGui.sameLine();
            ImGui.pushItemWidth(inputW);

            if (ImGui.inputInt("##PitchRange", data.pitchRange, 1)) {
                if (data.pitchRange.get() < 1)
                    data.pitchRange.set(1);
                if (data.pitchRange.get() > 24)
                    data.pitchRange.set(24);
                if (onDataChange != null)
                    onDataChange.run();
            }
            ImGui.popItemWidth();

            ImGui.tableNextRow(ImGuiTableRowFlags.None, sectionTotalH);
            ImGui.tableSetColumnIndex(0);

            ImGui.text("音高线图：");
            GraphWidget.render(cellW, finalGraphH, data.pitchCurve, 0xFF00FFAA, "Pitch", data.pitchRange.get(),
                    data.volumeCurve.size(), onDataChange);

            ImGui.endTable();
        }
    }

    private static void renderControls(NoteData data, Runnable onDataChange) {
        ImGui.dummy(0, 10.0f);
        if (ImGui.beginTable("SelectorLayout", 2)) {
            ImGui.tableNextColumn();
            ImGui.alignTextToFramePadding();
            ImGui.text("选择音色：");
            ImGui.sameLine();
            ImGui.setNextItemWidth(-1);

            if (ImGui.combo("##Cat", data.currentCategoryIndex, InstrumentBlockRegistry.CATEGORIES)) {
                updateInstrumentList(data);
                data.currentInstrumentIndex.set(0);
                updateInstrumentFromSelection(data);
                if (onDataChange != null)
                    onDataChange.run();
            }

            ImGui.tableNextColumn();
            ImGui.alignTextToFramePadding();
            ImGui.sameLine();
            ImGui.setNextItemWidth(-1);

            if (ImGui.combo("##Inst", data.currentInstrumentIndex, data.currentInstrumentList)) {
                updateInstrumentFromSelection(data);
                if (onDataChange != null)
                    onDataChange.run();
            }
            ImGui.endTable();
        }

        ImGui.dummy(0, 10.0f);
        ImGui.alignTextToFramePadding();
        ImGui.text("激活后延迟播放的毫秒数（范围：0-5000ms）：");
        ImGui.sameLine();
        ImGui.setNextItemWidth(-1);

        if (ImGui.inputInt("##Delay", data.delay, 1, 10)) {
            if (data.delay.get() < 0)
                data.delay.set(0);
            if (data.delay.get() > 5000)
                data.delay.set(5000);
            if (onDataChange != null)
                onDataChange.run();
        }

        // [已恢复] 辅助计算毫秒数的代码
        ImGui.dummy(0, 5.0f);
        float calcInputW = 75.0f;
        ImGui.alignTextToFramePadding();
        ImGui.text("tick rate：");
        ImGui.sameLine();
        ImGui.pushItemWidth(calcInputW);
        ImGui.inputFloat("##CalcRate", NewNoteBlockScreen.calcTickRate, 0, 0, "%.1f");
        ImGui.popItemWidth();
        ImGui.sameLine();
        ImGui.text("延迟播放tick数：");
        ImGui.sameLine();
        ImGui.pushItemWidth(calcInputW);
        ImGui.inputFloat("##CalcTicks", NewNoteBlockScreen.calcDelayTicks, 0, 0, "%.1f");
        ImGui.popItemWidth();

        float rate = NewNoteBlockScreen.calcTickRate.get();
        if (rate < 0.1f)
            rate = 0.1f;
        float ticks = NewNoteBlockScreen.calcDelayTicks.get();
        int resultMs = (int) ((50.0f * 20.0f / rate) * ticks);
        ImGui.sameLine();
        ImGui.text(" = " + resultMs + "ms");
        ImGui.sameLine();

        if (ImGui.button("应用计算结果")) {
            int finalVal = Math.max(0, Math.min(5000, resultMs));
            data.delay.set(finalVal);
            if (onDataChange != null)
                onDataChange.run();
        }

        ImGui.dummy(0, 10.0f);
        ImGui.separator();
        ImGui.alignTextToFramePadding();
        ImGui.text("频谱图预览:");

        float graphW = ImGui.getContentRegionAvailX();
        float availH = ImGui.getContentRegionAvailY();
        float graphH = Math.max(50.0f, availH - 5.0f);

        ImGui.pushStyleColor(ImGuiCol.ChildBg, 0.12f, 0.12f, 0.15f, 1.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);

        if (ImGui.beginChild("SpectrumRegion", graphW, graphH, true, ImGuiWindowFlags.HorizontalScrollbar)) {
            renderSpectrumGraph(graphW, graphH);
        }
        ImGui.endChild();
        ImGui.popStyleVar();
        ImGui.popStyleColor();
    }

    public static void updateInstrumentList(NoteData data) {
        String cat = InstrumentBlockRegistry.CATEGORIES[data.currentCategoryIndex.get()];
        LinkedHashMap<String, String> map = InstrumentBlockRegistry.UI_INSTRUMENT_MAP.get(cat);
        if (map != null) {
            data.currentInstrumentList = map.keySet().toArray(new String[0]);
        } else {
            data.currentInstrumentList = new String[0];
        }
    }

    public static void updateInstrumentFromSelection(NoteData data) {
        if (data.currentInstrumentList.length > 0
                && data.currentInstrumentIndex.get() < data.currentInstrumentList.length) {
            String cat = InstrumentBlockRegistry.CATEGORIES[data.currentCategoryIndex.get()];
            String displayName = data.currentInstrumentList[data.currentInstrumentIndex.get()];
            String id = InstrumentBlockRegistry.UI_INSTRUMENT_MAP.get(cat).get(displayName);
            data.instrument = id;
        }
    }

    private static void renderSpectrumGraph(float w, float h) {
        ImDrawList drawList = ImGui.getWindowDrawList();
        float startX = ImGui.getCursorScreenPosX();
        float startY = ImGui.getCursorScreenPosY();

        ImGui.dummy(w, h);
        drawGrid(drawList, startX, startY, w, h);

        if (NewNoteBlockScreen.cachedSpectrumData != null) {
            float[] data = NewNoteBlockScreen.cachedSpectrumData;
            drawSpectrumCurve(drawList, data, data.length, startX, startY, w, h);
            handleTooltip(data, data.length, startX, startY, w, h);
        }
    }

    private static void drawGrid(ImDrawList drawList, float x, float y, float w, float h) {
        int gridColor = ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.1f);
        int textColor = ImGui.getColorU32(0.6f, 0.6f, 0.6f, 0.5f);
        float[] freqs = { 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000 };
        String[] labels = { "20", "50", "100", "200", "500", "1k", "2k", "5k", "10k", "20k" };
        double minLog = Math.log10(20);
        double maxLog = Math.log10(20000);
        double rangeLog = maxLog - minLog;
        for (int i = 0; i < freqs.length; i++) {
            double f = Math.log10(freqs[i]);
            float ratio = (float) ((f - minLog) / rangeLog);
            float lineX = x + ratio * w;
            drawList.addLine(lineX, y, lineX, y + h, gridColor);
            drawList.addText(lineX + 2, y + h - 25, textColor, labels[i]);
        }
        for (int i = 1; i < 6; i++) {
            float ratio = i / 6.0f;
            float lineY = y + ratio * h;
            drawList.addLine(x, lineY, x + w, lineY, gridColor);
        }
    }

    private static void drawSpectrumCurve(ImDrawList drawList, float[] data, int count, float x, float y, float w,
            float h) {
        int colTop = ImGui.getColorU32(0.8f, 0.3f, 0.9f, 0.6f);
        int strokeCol = ImGui.getColorU32(1.0f, 0.6f, 1.0f, 1.0f);
        int segments = count - 1;
        int subdiv = 3;
        for (int i = 0; i < segments; i++) {
            float v0 = (i > 0) ? clamp(data[i - 1]) : clamp(data[i]);
            float v1 = clamp(data[i]);
            float v2 = clamp(data[i + 1]);
            float v3 = (i < segments - 1) ? clamp(data[i + 2]) : clamp(data[i + 1]);
            for (int j = 0; j <= subdiv; j++) {
                float t1 = j / (float) (subdiv + 1);
                float val1 = catmullRom(v0, v1, v2, v3, t1);
                float globalT1 = (i + t1) / segments;
                float px1 = x + globalT1 * w;
                float py1 = y + h - (Math.max(0.0f, Math.min(1.0f, val1)) * h);
                float t2 = (j + 1) / (float) (subdiv + 1);
                float val2 = catmullRom(v0, v1, v2, v3, t2);
                float globalT2 = (i + t2) / segments;
                float px2 = x + globalT2 * w;
                float py2 = y + h - (Math.max(0.0f, Math.min(1.0f, val2)) * h);
                drawList.addQuadFilled(px1, py1, px2, py2, px2, y + h, px1, y + h, colTop);
            }
        }
        drawList.pathClear();
        float firstY = y + h - (clamp(data[0]) * h);
        drawList.pathLineTo(x, firstY);
        for (int i = 0; i < segments; i++) {
            float v0 = (i > 0) ? clamp(data[i - 1]) : clamp(data[i]);
            float v1 = clamp(data[i]);
            float v2 = clamp(data[i + 1]);
            float v3 = (i < segments - 1) ? clamp(data[i + 2]) : clamp(data[i + 1]);
            for (int j = 1; j <= subdiv; j++) {
                float t = j / (float) (subdiv + 1);
                float val = catmullRom(v0, v1, v2, v3, t);
                float globalT = (i + t) / segments;
                float px = x + globalT * w;
                float py = y + h - (Math.max(0.0f, Math.min(1.0f, val)) * h);
                drawList.pathLineTo(px, py);
            }
            float nextMainY = y + h - (clamp(data[i + 1]) * h);
            drawList.pathLineTo(x + ((i + 1) / (float) segments) * w, nextMainY);
        }
        drawList.pathStroke(strokeCol, 0, 2.0f);
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float v0 = (p2 - p0) * 0.5f;
        float v1 = (p3 - p1) * 0.5f;
        float t2 = t * t;
        float t3 = t * t2;
        return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * t + p1;
    }

    private static void handleTooltip(float[] data, int count, float x, float y, float w, float h) {
        if (ImGui.isWindowHovered()) {
            float mouseX = ImGui.getMousePosX();
            float relativeX = mouseX - x;
            if (relativeX >= 0 && relativeX <= w) {
                float normalizedX = relativeX / w;
                double minLog = Math.log10(20);
                double maxLog = Math.log10(20000);
                double fLog = minLog + normalizedX * (maxLog - minLog);
                double freq = Math.pow(10, fLog);
                ImGui.setTooltip(String.format("%.0f Hz", freq));
                ImGui.getWindowDrawList().addLine(mouseX, y, mouseX, y + h, 0x88FFFFFF);
            }
        }
    }

    private static float clamp(float val) {
        return Math.max(0.0f, Math.min(1.0f, val));
    }
}