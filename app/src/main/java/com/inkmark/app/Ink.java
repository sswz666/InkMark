package com.inkmark.app;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

/** 一笔（或一个图形）。 */
public final class Ink {

    public final Tool tool;
    public final int color;
    /** 已经按工具系数换算过的线宽（px）。 */
    public final float width;
    public final ArrayList<PointF> points = new ArrayList<>(48);

    public Ink(Tool tool, int color, float width) {
        this.tool = tool;
        this.color = color;
        this.width = width;
    }

    public void add(float x, float y) {
        if (tool.isShape()) {
            // 图形只保留起点和当前点
            if (points.isEmpty()) {
                points.add(new PointF(x, y));
                points.add(new PointF(x, y));
            } else {
                points.set(1, new PointF(x, y));
            }
        } else {
            points.add(new PointF(x, y));
        }
    }

    public List<PointF> points() {
        return points;
    }

    public float sx() {
        return points.isEmpty() ? 0f : points.get(0).x;
    }

    public float sy() {
        return points.isEmpty() ? 0f : points.get(0).y;
    }

    public float ex() {
        return points.isEmpty() ? 0f : points.get(points.size() - 1).x;
    }

    public float ey() {
        return points.isEmpty() ? 0f : points.get(points.size() - 1).y;
    }

    /** 图形长度为 0（只是点了一下）时视为无效。 */
    public boolean isDegenerate(float minLength) {
        if (!tool.isShape()) {
            return points.isEmpty();
        }
        return Math.hypot(ex() - sx(), ey() - sy()) < minLength;
    }
}
