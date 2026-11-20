package com.inkmark.app;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

/** 一笔（或一个图形、一段文字）。 */
public final class Ink {

    public final Tool tool;
    public final int color;
    /** 已经按工具系数换算过的线宽（px）。 */
    public final float width;
    public final ArrayList<PointF> points = new ArrayList<>(48);

    /** 文本内容（仅 tool == TEXT 时非空）。 */
    public String text;
    /** 文字字号（px）。 */
    public float textSize;
    /** 文字排版后的包围盒尺寸（px），由 CanvasView 在插入时量好，命中检测直接用。 */
    public float textWidth;
    public float textHeight;

    public Ink(Tool tool, int color, float width) {
        this.tool = tool;
        this.color = color;
        this.width = width;
    }

    /** 一段落在 (x, y) 左上角的文字。 */
    public static Ink text(int color, float textSize, float x, float y, String content) {
        Ink ink = new Ink(Tool.TEXT, color, Math.max(1f, textSize * 0.08f));
        ink.text = content;
        ink.textSize = textSize;
        ink.points.add(new PointF(x, y));
        return ink;
    }

    public boolean isText() {
        return tool == Tool.TEXT && text != null && !text.isEmpty();
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
