package com.chunfeng.newnoteblock.client.ui.widget;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;

import java.util.ArrayList;

public class GraphWidget {
    private static boolean isDraggingVolGraph = false;
    private static boolean isDraggingPitchGraph = false;

    private static final float TICK_LABEL_HEIGHT = 30.0f;
    private static final float MIN_PIXELS_PER_SEMITONE = 25.0f;
    private static final float MIN_PIXELS_PER_TICK = 50.0f;
    private static final float SCROLLBAR_HEIGHT = 20.0f; // 横向滚动条占用的高度

    public static void resetDragging() {
        isDraggingVolGraph = false;
        isDraggingPitchGraph = false;
    }

    /**
     * @param onDataChange 当数据发生改变时的回调 (通常用于发送网络包)
     */
    public static void render(float w, float h, ArrayList<Integer> data, int pointColor, String labelPrefix,
            int pitchRange, int refCurveSize, Runnable onDataChange) {
        // 获取起始绝对坐标
        float startX = ImGui.getCursorScreenPosX();
        float startY = ImGui.getCursorScreenPosY();

        // --- 逻辑分支 1: 空数据 ---
        if (data == null || data.isEmpty()) {
            var drawList = ImGui.getWindowDrawList();
            drawList.addRectFilled(startX, startY, startX + w, startY + h, 0xFF222222);
            drawList.addRect(startX, startY, startX + w, startY + h, 0xFF888888);

            String text = "无包络数据(设置持续时长添加)";
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
            while (data.size() < refCurveSize)
                data.add(0);
            while (data.size() > refCurveSize)
                data.remove(data.size() - 1);
        }

        int totalTicks = data.size();

        // [新增] 计算横向内容宽度：确保每个 tick 至少有 MIN_PIXELS_PER_TICK 像素
        float contentWidth = Math.max(w, totalTicks * MIN_PIXELS_PER_TICK);

        // 纵向滚动（Pitch 图的音高范围）
        boolean verticalScrollable = labelPrefix.equals("Pitch") && pitchRange > 1;
        float contentHeight = h;
        if (verticalScrollable) {
            contentHeight = (pitchRange * 2) * MIN_PIXELS_PER_SEMITONE;
            if (contentHeight < h)
                contentHeight = h;
        }

        // [修改] 始终启用横向滚动条
        int childFlags = ImGuiWindowFlags.HorizontalScrollbar;

        // [修复] 子窗口高度 = 图表高度 + tick 标签高度 + 滚动条高度（如果需要）
        boolean needsHScroll = contentWidth > w;
        float childH = h + TICK_LABEL_HEIGHT + (needsHScroll ? SCROLLBAR_HEIGHT : 0);
        ImGui.beginChild("GraphChild_" + labelPrefix, w, childH, false, childFlags);

        var childDrawList = ImGui.getWindowDrawList();
        float canvasX = ImGui.getCursorScreenPosX();
        float canvasY = ImGui.getCursorScreenPosY();

        // [修改] 使用 contentWidth 作为 dummy 宽度，触发横向滚动
        ImGui.dummy(contentWidth, contentHeight + TICK_LABEL_HEIGHT);

        childDrawList.addRectFilled(canvasX, canvasY, canvasX + contentWidth, canvasY + contentHeight, 0xFF222222);
        childDrawList.addRect(canvasX, canvasY, canvasX + contentWidth, canvasY + contentHeight, 0xFF888888);

        float stepX = contentWidth / (float) totalTicks;

        float mouseX = ImGui.getMousePosX();
        float mouseY = ImGui.getMousePosY();

        // [修复] 只检测图表画布区域内的鼠标操作，排除滚动条区域
        boolean isHoveringView = mouseX >= canvasX && mouseX <= canvasX + contentWidth
                && mouseY >= canvasY && mouseY <= canvasY + contentHeight;
        boolean isMouseDown = ImGui.isMouseDown(0);

        boolean isThisGraphDragging = false;
        if (labelPrefix.equals("Vol")) {
            if (isHoveringView && ImGui.isMouseClicked(0))
                isDraggingVolGraph = true;
            if (!isMouseDown)
                isDraggingVolGraph = false;
            isThisGraphDragging = isDraggingVolGraph;
        } else {
            if (isHoveringView && ImGui.isMouseClicked(0))
                isDraggingPitchGraph = true;
            if (!isMouseDown)
                isDraggingPitchGraph = false;
            isThisGraphDragging = isDraggingPitchGraph;
        }

        if (isThisGraphDragging) {
            float relativeX = Math.max(0, Math.min(mouseX - canvasX, contentWidth - 0.01f));
            int index = (int) (relativeX / stepX);
            index = Math.max(0, Math.min(index, totalTicks - 1));

            int newVal;
            if (labelPrefix.equals("Pitch")) {
                float midY = canvasY + (contentHeight / 2.0f);
                float pxPerSemi = contentHeight / (2.0f * pitchRange);
                float distFromMid = midY - mouseY;
                float semitones = distFromMid / pxPerSemi;
                newVal = (int) (semitones * 100.0f);
                int maxCents = pitchRange * 100;
                newVal = Math.max(-maxCents, Math.min(newVal, maxCents));
            } else {
                float relativeY = Math.max(0, Math.min(mouseY - canvasY, contentHeight));
                newVal = (int) (100.0f * (1.0f - (relativeY / contentHeight)));
                newVal = Math.max(0, Math.min(newVal, 100));
            }

            if (data.get(index) != newVal) {
                data.set(index, newVal);
                if (onDataChange != null)
                    onDataChange.run();
            }
        }

        // 绘制网格线
        for (int i = 1; i < totalTicks; i++) {
            float px = canvasX + (i * stepX);
            childDrawList.addLine(px, canvasY, px, canvasY + contentHeight, 0xFF444444, 1.0f);
        }

        // Pitch 中线和半音线
        if (labelPrefix.equals("Pitch")) {
            float midY = canvasY + (contentHeight / 2.0f);
            childDrawList.addLine(canvasX, midY, canvasX + contentWidth, midY, 0xFF888888, 2.0f);

            if (pitchRange > 0) {
                float pxPerSemi = contentHeight / (2.0f * pitchRange);
                for (int s = 1; s <= pitchRange; s++) {
                    float lineYUp = midY - (s * pxPerSemi);
                    childDrawList.addLine(canvasX, lineYUp, canvasX + contentWidth, lineYUp, 0x44FFFFFF, 1.0f);
                    float lineYDown = midY + (s * pxPerSemi);
                    childDrawList.addLine(canvasX, lineYDown, canvasX + contentWidth, lineYDown, 0x44FFFFFF, 1.0f);
                }
            }
        }

        // 绘制数据点和连线
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

        // [修改] Tick 标签绘制在滚动区域内部（跟随横向滚动）
        float textY = canvasY + contentHeight + 2;
        for (int i = 0; i < totalTicks; i++) {
            int stepDisplay = totalTicks > 20 ? 5 : 1;
            if ((i + 1) % stepDisplay == 0 || i == 0) {
                String labelStr = String.valueOf(i + 1);
                ImVec2 textSize = new ImVec2();
                ImGui.calcTextSize(textSize, labelStr);

                float px = canvasX + (i * stepX);
                float cellCenterX = px + (stepX / 2.0f);
                float textX = cellCenterX - (textSize.x / 2.0f);

                childDrawList.addText(textX, textY, 0xFFFFFFFF, labelStr);
            }
        }

        // Tooltip
        if (isHoveringView) {
            float relativeX = Math.max(0, Math.min(mouseX - canvasX, contentWidth - 0.01f));
            int index = (int) (relativeX / stepX);

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

        ImGui.endChild();
    }
}