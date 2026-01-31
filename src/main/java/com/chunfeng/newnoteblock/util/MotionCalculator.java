package com.chunfeng.newnoteblock.util;

import net.minecraft.util.math.Vec3d;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.List;

public class MotionCalculator {

    public static List<Vec3d> calculate(String expXStr, String expYStr, String expZStr, int startTick, int endTick)
            throws Exception {
        List<Vec3d> path = new ArrayList<>();

        if (startTick > endTick) {
            throw new IllegalArgumentException("起始 Tick 不能大于终止 Tick");
        }

        Expression expX = new ExpressionBuilder(expXStr).variable("t").build();
        Expression expY = new ExpressionBuilder(expYStr).variable("t").build();
        Expression expZ = new ExpressionBuilder(expZStr).variable("t").build();

        for (int t = startTick; t <= endTick; t++) {
            for (float subT = 0; subT < 1.0f; subT += 0.2f) {
                double time = t + subT;
                if (time > endTick)
                    break;

                expX.setVariable("t", time);
                expY.setVariable("t", time);
                expZ.setVariable("t", time);

                double x = expX.evaluate();
                double y = expY.evaluate();
                double z = expZ.evaluate();

                path.add(new Vec3d(x, y, z));
            }
        }
        return path;
    }
}
