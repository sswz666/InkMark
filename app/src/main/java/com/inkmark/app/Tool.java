package com.inkmark.app;

/** 标注工具。 */
public enum Tool {

    PEN("pen", R.string.tool_pen, R.drawable.ic_pen, 1.0f),
    MARKER("marker", R.string.tool_marker, R.drawable.ic_marker, 2.6f),
    LINE("line", R.string.tool_line, R.drawable.ic_line, 1.0f),
    ARROW("arrow", R.string.tool_arrow, R.drawable.ic_arrow, 1.0f),
    RECT("rect", R.string.tool_rect, R.drawable.ic_rect, 1.0f),
    ELLIPSE("ellipse", R.string.tool_ellipse, R.drawable.ic_ellipse, 1.0f),
    LASER("laser", R.string.tool_laser, R.drawable.ic_laser, 1.35f),
    ERASER("eraser", R.string.tool_eraser, R.drawable.ic_eraser, 1.0f);

    public final String key;
    public final int labelRes;
    public final int iconRes;
    public final float widthFactor;

    Tool(String key, int labelRes, int iconRes, float widthFactor) {
        this.key = key;
        this.labelRes = labelRes;
        this.iconRes = iconRes;
        this.widthFactor = widthFactor;
    }

    public boolean isShape() {
        return this == LINE || this == ARROW || this == RECT || this == ELLIPSE;
    }

    public boolean isFreehand() {
        return this == PEN || this == MARKER;
    }

    public static Tool fromKey(String key) {
        if (key != null) {
            for (Tool t : values()) {
                if (t.key.equals(key)) {
                    return t;
                }
            }
        }
        return PEN;
    }

    /** 主面板里展示的顺序。 */
    public static final Tool[] ORDER = {
            PEN, MARKER, ARROW, RECT, ELLIPSE, LINE, LASER, ERASER
    };

    /** 标注工具条里的顺序（7 个，橡皮放在右侧操作列）。 */
    public static final Tool[] BAR_ORDER = {
            PEN, MARKER, LINE, ARROW, RECT, ELLIPSE, LASER
    };
}
