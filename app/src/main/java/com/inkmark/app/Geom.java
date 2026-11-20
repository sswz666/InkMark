package com.inkmark.app;

/**
 * 纯数学工具：距离计算用于橡皮擦命中判定。
 * 不依赖任何 Android API，便于在 JVM 上做单元测试。
 */
public final class Geom {

    private Geom() {
    }

    /** 点到线段的最短距离。 */
    public static float distToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len2 = dx * dx + dy * dy;
        float t = 0f;
        if (len2 > 0f) {
            t = ((px - x1) * dx + (py - y1) * dy) / len2;
            t = Math.max(0f, Math.min(1f, t));
        }
        float cx = x1 + t * dx;
        float cy = y1 + t * dy;
        return (float) Math.hypot(px - cx, py - cy);
    }

    /** 点到矩形边框的最短距离（点在内部时返回内部，点在外部时返回到边框的距离）。 */
    public static float distToRectEdge(float px, float py, float left, float top, float right, float bottom) {
        if (right < left) {
            float tmp = left;
            left = right;
            right = tmp;
        }
        if (bottom < top) {
            float tmp = top;
            top = bottom;
            bottom = tmp;
        }
        boolean inside = px >= left && px <= right && py >= top && py <= bottom;
        if (inside) {
            float dl = px - left;
            float dr = right - px;
            float dt = py - top;
            float db = bottom - py;
            return Math.min(Math.min(dl, dr), Math.min(dt, db));
        }
        float dx = Math.max(Math.max(left - px, 0f), px - right);
        float dy = Math.max(Math.max(top - py, 0f), py - bottom);
        return (float) Math.hypot(dx, dy);
    }

    /** 点到椭圆轮廓的近似距离。 */
    public static float distToEllipseOutline(float px, float py, float cx, float cy, float rx, float ry) {
        if (rx <= 0f || ry <= 0f) {
            return Float.MAX_VALUE;
        }
        float nx = (px - cx) / rx;
        float ny = (py - cy) / ry;
        float d = (float) Math.hypot(nx, ny);
        return Math.abs(d - 1f) * Math.min(rx, ry);
    }

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
