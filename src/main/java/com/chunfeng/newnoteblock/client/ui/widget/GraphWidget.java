package com.chunfeng.newnoteblock.client.ui.widget;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;

import java.util.ArrayList;

public class GraphWidget {
    private static boolean isDraggingVolGraph = false;
    private static boolean isDraggingPitchGraph = false;

    private static final float TICK_LABEL_HEIGHT = 20.0f;
    private static final float MIN_PIXELS_PER_SEMITONE = 25.0f;

    public static void resetDragging() {
        isDraggingVolGraph = false;
        isDraggingPitchGraph = false;
    }

    /**
     * @param onDataChange 当数据发生改变时的回调 (通常用于发送网络包)
     */
    public static void render(float w, float h, ArrayList<Integer> data, int pointColor, String labelPrefix, int pitchRange, int refCurveSize, Runnable onDataChange) {
        // 获取起始绝对坐标
        float startX = ImGui.getCursorScreenPosX();
        float startY = ImGui.getCursorScreenPosY();

        // --- 逻辑分支 1: 空数据 ---
        if (data == null || data.isEmpty()) {
            var drawList = ImGui.getWindowDrawList();
            drawList.addRectFilled(startX, startY, startX + w, startY + h, 0xFF222222);
            drawList.addRect(startX, startY, startX + w, startY + h, 0xFF888888);

            String text = "无包络数据(拖动滑块添加)";
            ImVec2 textSize = new ImVec2();
            ImGui.calcTextSize(textSize, text);

            float tx = startX + (w - textSize.x) / 2.0f;
            float ty = startY + (h - textSize.y) / 2.0f;
            drawList.addText(tx, ty, 0xFF888888, text);

            ImGui.dummy(w, h + TICK_LABEL_HEIGHT);
            return;
        }

        // --- 逻辑分支 2: 绘制图表 ---

        // 同步列表长度 (仅针对 Pitch 曲线需要参考 Vol 曲线长度)
        if (labelPrefix.equals("Pitch") && refCurveSize > 0) {
            while (data.size() < refCurveSize) data.add(0);
            while (data.size() > refCurveSize) data.remove(data.size()-1);
        }

        boolean scrollable = labelPrefix.equals("Pitch") && pitchRange > 1;

        float contentHeight = h;
        if (scrollable) {
            contentHeight = (pitchRange * 2) * MIN_PIXELS_PER_SEMITONE;
            if (contentHeight < h) contentHeight = h;
        }

        ImGui.beginChild("GraphChild_" + labelPrefix, w, h, false, ImGuiWindowFlags.HorizontalScrollbar | (scrollable ? 0 : ImGuiWindowFlags.NoScrollbar));

        var childDrawList = ImGui.getWindowDrawList();
        float canvasX = ImGui.getCursorScreenPosX();
        float canvasY = ImGui.getCursorScreenPosY();

        ImGui.dummy(w, contentHeight);

        childDrawList.addRectFilled(canvasX, canvasY, canvasX + w, canvasY + contentHeight, 0xFF222222);
        childDrawList.addRect(canvasX, canvasY, canvasX + w, canvasY + contentHeight, 0xFF888888);

        int totalTicks = data.size();
        float stepX = w / (float)totalTicks;

        float winX = ImGui.getWindowPosX();
        float winY = ImGui.getWindowPosY();
        float winW = ImGui.getWindowWidth();
        float winH = ImGui.getWindowHeight();
        float mouseX = ImGui.getMousePosX();
        float mouseY = ImGui.getMousePosY();

        boolean isHoveringView = mouseX >= winX && mouseX <= winX + winW && mouseY >= winY && mouseY <= winY + winH;
        boolean isMouseDown = ImGui.isMouseDown(0);

        boolean isThisGraphDragging = false;
        if (labelPrefix.equals("Vol")) {
            if (isHoveringView && ImGui.isMouseClicked(0)) isDraggingVolGraph = true;
            if (!isMouseDown) isDraggingVolGraph = false;
            isThisGraphDragging = isDraggingVolGraph;
        } else {
            if (isHoveringView && ImGui.isMouseClicked(0)) isDraggingPitchGraph = true;
            if (!isMouseDown) isDraggingPitchGraph = false;
            isThisGraphDragging = isDraggingPitchGraph;
        }

        if (isThisGraphDragging) {
            float relativeX = Math.max(0, Math.min(mouseX - canvasX, w - 0.01f));
            int index = (int)(relativeX / stepX);
            index = Math.max(0, Math.min(index, totalTicks - 1));

            int newVal;
            if (labelPrefix.equals("Pitch")) {
                float midY = canvasY + (contentHeight / 2.0f);
                float pxPerSemi = contentHeight / (2.0f * pitchRange);
                float distFromMid = midY - mouseY;
                float semitones = distFromMid / pxPerSemi;
                newVal = (int)(semitones * 100.0f);
                int maxCents = pitchRange * 100;
                newVal = Math.max(-maxCents, Math.min(newVal, maxCents));
            } else {
                float relativeY = Math.max(0, Math.min(mouseY - canvasY, contentHeight));
                newVal = (int)(100.0f * (1.0f - (relativeY / contentHeight)));
                newVal = Math.max(0, Math.min(newVal, 100));
            }

            if (data.get(index) != newVal) {
                data.set(index, newVal);
                if (onDataChange != null) onDataChange.run();
            }
        }

        for (int i = 1; i < totalTicks; i++) {
            float px = canvasX + (i * stepX);
            childDrawList.addLine(px, canvasY, px, canvasY + contentHeight, 0xFF444444, 1.0f);
        }

        if (labelPrefix.equals("Pitch")) {
            float midY = canvasY + (contentHeight / 2.0f);
            childDrawList.addLine(canvasX, midY, canvasX + w, midY, 0xFF888888, 2.0f);

            if (pitchRange > 0) {
                float pxPerSemi = contentHeight / (2.0f * pitchRange);
                for (int s = 1; s <= pitchRange; s++) {
                    float lineYUp = midY - (s * pxPerSemi);
                    childDrawList.addLine(canvasX, lineYUp, canvasX + w, lineYUp, 0x44FFFFFF, 1.0f);
                    float lineYDown = midY + (s * pxPerSemi);
                    childDrawList.addLine(canvasX, lineYDown, canvasX + w, lineYDown, 0x44FFFFFF, 1.0f);
                }
            }
        }

        for (int i = 0; i < totalTicks; i++) {
            float px = canvasX + (i * stepX);
            float pointCenterX = px + (stepX / 2.0f);
            float py;

            if (labelPrefix.equals("Pitch")) {
                float midY = canvasY + (contentHeight / 2.0f);
                float pxPerSemi = contentHeight / (2.0f * pitchRange);
                float semitones = data.get(i) / 100.0f;
                py = midY - (semitones * pxPerSemi);
            } else {
                py = canvasY + contentHeight - (data.get(i) / 100.0f * contentHeight);
            }

            childDrawList.addCircleFilled(pointCenterX, py, 4.0f, pointColor);

            if (i < totalTicks - 1) {
                float nx = canvasX + ((i + 1) * stepX);
                float nextPointCenterX = nx + (stepX / 2.0f);
                float ny;
                if (labelPrefix.equals("Pitch")) {
                    float midY = canvasY + (contentHeight / 2.0f);
                    float pxPerSemi = contentHeight / (2.0f * pitchRange);
                    float semitones = data.get(i + 1) / 100.0f;
                    ny = midY - (semitones * pxPerSemi);
                } else {
                    ny = canvasY + contentHeight - (data.get(i + 1) / 100.0f * contentHeight);
                }
                childDrawList.addLine(pointCenterX, py, nextPointCenterX, ny, pointColor, 2.0f);
            }
        }

        ImGui.endChild();

        var winDrawList = ImGui.getWindowDrawList();
        float textY = startY + h + 2;

        for (int i = 0; i < totalTicks; i++) {
            int stepDisplay = totalTicks > 20 ? 5 : 1;
            if ((i + 1) % stepDisplay == 0 || i == 0) {
                String labelStr = String.valueOf(i + 1);
                ImVec2 textSize = new ImVec2();
                ImGui.calcTextSize(textSize, labelStr);

                float px = startX + (i * stepX);
                float cellCenterX = px + (stepX / 2.0f);
                float textX = cellCenterX - (textSize.x / 2.0f);

                if (textX >= startX && textX + textSize.x <= startX + w) {
                    winDrawList.addText(textX, textY, 0xFFFFFFFF, labelStr);
                }
            }
        }

        if (isHoveringView) {
            float relativeX = Math.max(0, Math.min(mouseX - canvasX, w - 0.01f));
            int index = (int)(relativeX / stepX);

            if (index >= 0 && index < data.size()) {
                int val = data.get(index);
                String tipText;
                if (labelPrefix.equals("Vol")) {
                    tipText = String.format("音量 - 第 %d tick | 值: %d%%", index + 1, val);
                } else {
                    tipText = String.format("音高 - 第 %d tick | 偏移: %d 音分", index + 1, val);
                }
                ImGui.setTooltip(tipText);
            }
        }

        ImGui.dummy(w, TICK_LABEL_HEIGHT);
    }
}