package com.inkmark.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.util.TypedValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * 校验"透明弹窗"方案的关键窗口属性：
 * 主面板 = 系统 Dialog 浮动窗口（不铺满、不需要启动窗、走 popup 动画）；
 * 标注层 = 全屏半透明窗口（下层可见）。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WindowStyleTest {

    private static boolean attr(Activity activity, int attrId) {
        TypedValue value = new TypedValue();
        boolean found = activity.getTheme().resolveAttribute(attrId, value, true);
        return found && value.data != 0;
    }

    private static int attrResource(Activity activity, int attrId) {
        TypedValue value = new TypedValue();
        activity.getTheme().resolveAttribute(attrId, value, true);
        return value.resourceId;
    }

    @Test
    public void mainCardUsesFloatingDialogWindow() {
        Activity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        assertTrue("主面板必须是浮动窗口（windowIsFloating）",
                attr(activity, android.R.attr.windowIsFloating));
        assertTrue("主面板窗口必须可透明（windowIsTranslucent）",
                attr(activity, android.R.attr.windowIsTranslucent));
        assertEquals("必须使用 220ms 上浮淡入动画",
                R.style.Anim_InkMark_Popup,
                attrResource(activity, android.R.attr.windowAnimationStyle));
        assertTrue("卡片外点击应关闭",
                attr(activity, android.R.attr.windowCloseOnTouchOutside));
    }

    @Test
    public void annotationLayerIsTranslucentFullscreen() {
        Activity activity = Robolectric.buildActivity(AnnotateActivity.class).setup().get();

        assertTrue("标注层必须透明（下层可见）",
                attr(activity, android.R.attr.windowIsTranslucent));
        assertTrue("标注层不能有窗口背景",
                attrResource(activity, android.R.attr.windowBackground) == android.R.color.transparent);
        assertEquals("标注层同样使用 popup 动画",
                R.style.Anim_InkMark_Popup,
                attrResource(activity, android.R.attr.windowAnimationStyle));
        assertTrue("标注层不加暗背景", !attr(activity, android.R.attr.backgroundDimEnabled));
    }

    @Test
    public void canvasStartsEmptyAndUndoIsDisabled() {
        AnnotateActivity activity = Robolectric.buildActivity(AnnotateActivity.class).setup().get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        assertTrue(!canvas.hasInk());
        assertTrue(!canvas.undo());
        assertTrue(!canvas.redo());
    }

    @Test
    @Config(sdk = 34, qualifiers = "w411dp-h891dp-night-xxhdpi")
    public void nightModeUsesDarkSurface() {
        Activity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        int card = activity.getColor(R.color.card);
        // 深色模式下卡片底色应是深色（亮度低于 0.2）
        float[] hsl = new float[3];
        android.graphics.Color.colorToHSV(card, hsl);
        assertTrue("夜间卡片应为深色", hsl[2] < 0.25f);
    }
}
