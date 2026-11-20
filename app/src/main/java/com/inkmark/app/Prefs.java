package com.inkmark.app;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private static final String FILE = "inkmark";
    private static final String KEY_TOOL = "tool";
    private static final String KEY_COLOR = "color";
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HIDE = "hide_recents";

    private Prefs() {
    }

    private static SharedPreferences sp(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static Tool tool(Context context) {
        return Tool.fromKey(sp(context).getString(KEY_TOOL, Tool.PEN.key));
    }

    public static void setTool(Context context, Tool tool) {
        sp(context).edit().putString(KEY_TOOL, tool.key).apply();
    }

    public static int color(Context context) {
        return sp(context).getInt(KEY_COLOR, 0xFF1B1B1F);
    }

    public static void setColor(Context context, int color) {
        sp(context).edit().putInt(KEY_COLOR, color).apply();
    }

    public static int widthLevel(Context context) {
        return sp(context).getInt(KEY_WIDTH, 1);
    }

    public static void setWidthLevel(Context context, int level) {
        sp(context).edit().putInt(KEY_WIDTH, level).apply();
    }

    public static boolean hideRecents(Context context) {
        return sp(context).getBoolean(KEY_HIDE, true);
    }

    public static void setHideRecents(Context context, boolean hide) {
        sp(context).edit().putBoolean(KEY_HIDE, hide).apply();
    }

    /** 调色板：墨黑 / 白 / 蓝紫 / 青 / 橙 / 红 / 黄。 */
    public static final int[] PALETTE = {
            0xFF1B1B1F, 0xFFFFFFFF, 0xFF5B6BD8, 0xFF17BFA8,
            0xFFFF9F43, 0xFFFF5D6C, 0xFFFFC53D
    };
}
