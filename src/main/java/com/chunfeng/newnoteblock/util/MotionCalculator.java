package com.chunfeng.newnoteblock.util;

import net.minecraft.util.math.Vec3d;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.List;

public class MotionCalculator {

    /**
     * 计算运动轨迹路径
     * 
     * @param expXStr   X 轴表达式（可为 null 或空，将返回 0）
     * @param expYStr   Y 轴表达式（可为 null 或空，将返回 0）
     * @param expZStr   Z 轴表达式（可为 null 或空，将返回 0）
     * @param startTick 起始 tick
     * @param endTick   结束 tick
     * @return 轨迹点列表，每个 tick 一个点
     */
    public static List<Vec3d> calculate(String expXStr, String expYStr, String expZStr, int startTick, int endTick) {
        List<Vec3d> path = new ArrayList<>();

        if (startTick > endTick) {
            return path; // 返回空列表而不是抛异常
        }

        try {
            // 安全处理空表达式
            Expression expX = isValidExpression(expXStr)
                    ? new ExpressionBuilder(expXStr).variable("t").build()
                    : null;
            Expression expY = isValidExpression(expYStr)
                    ? new ExpressionBuilder(expYStr).variable("t").build()
                    : null;
            Expression expZ = isValidExpression(expZStr)
                    ? new ExpressionBuilder(expZStr).variable("t").build()
                    : null;

            for (int t = startTick; t <= endTick; t++) {
                double x = (expX != null) ? expX.setVariable("t", t).evaluate() : 0;
                double y = (expY != null) ? expY.setVariable("t", t).evaluate() : 0;
                double z = (expZ != null) ? expZ.setVariable("t", t).evaluate() : 0;
                path.add(new Vec3d(x, y, z));
            }
        } catch (Exception e) {
            // 解析失败时返回空列表
            path.clear();
        }
        return path;
    }

    private static boolean isValidExpression(String expr) {
        return expr != null && !expr.trim().isEmpty();
    }
}
