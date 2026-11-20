package com.inkmark.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 无模拟器界面测试：
 * 真实创建 Activity、inflate 布局、派发触摸事件，并把结果渲染成 PNG 便于人工检查。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class UiSmokeTest {

    /** 与 @Config 的 w411dp-h891dp-xxhdpi 对齐：411×3 = 1233，891×3 = 2673。 */
    private static final int W = 1233;
    private static final int H = 2673;

    private static File outputDir() {
        File dir = new File("build/ui-preview");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }

    @Test
    public void mainCardInflatesAndRenders() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();

        View start = activity.findViewById(R.id.btn_start);
        assertNotNull(start);
        assertNotNull(activity.findViewById(R.id.tool_grid));
        assertEquals(Tool.ORDER.length, ((ViewGroup) activity.findViewById(R.id.tool_grid)).getChildCount());
        assertEquals(Prefs.PALETTE.length, ((ViewGroup) activity.findViewById(R.id.color_row)).getChildCount());

        // 点击第二个工具格，确认选中态与偏好设置同步
        ViewGroup grid = activity.findViewById(R.id.tool_grid);
        grid.getChildAt(1).performClick();
        assertEquals(Tool.MARKER, Prefs.tool(activity));

        Bitmap bitmap = renderCard(activity);
        writePng(bitmap, "01-main-card.png");
    }

    @Test
    @Config(sdk = 34, qualifiers = "w411dp-h891dp-night-xxhdpi")
    public void mainCardRendersInNightMode() throws Exception {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        writePng(renderCard(activity), "03-main-card-night.png");
    }

    @Test
    public void annotationCanvasDrawsAndUndoes() throws Exception {
        ActivityController<AnnotateActivity> controller =
                Robolectric.buildActivity(AnnotateActivity.class).setup();
        AnnotateActivity activity = controller.get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        assertNotNull(canvas);

        layoutFullScreen(canvas);

        // 模拟一笔：按下 → 若干移动 → 抬起
        dispatchStroke(canvas, new float[]{200, 300, 400, 500, 600, 700, 800},
                new float[]{600, 620, 700, 640, 760, 680, 700});
        assertTrue(canvas.hasInk());

        // 再画一个箭头：切工具后按下拖动抬起
        View toolRow = activity.findViewById(R.id.tool_row);
        ((ViewGroup) toolRow).getChildAt(3).performClick();
        dispatchStroke(canvas, new float[]{250, 250}, new float[]{400, 1400});

        Bitmap inkLayer = canvas.exportBitmap();
        assertNotNull(inkLayer);
        assertTrue("墨迹层应有可见像素", alphaCount(inkLayer) > 20);

        assertTrue(canvas.undo());
        assertTrue(canvas.undo());
        assertFalse(canvas.hasInk());
        assertFalse(canvas.undo());

        // 重画几笔，用于预览图
        ((ViewGroup) toolRow).getChildAt(0).performClick();
        dispatchStroke(canvas, new float[]{180, 260, 360, 460, 560, 660, 760},
                new float[]{640, 660, 740, 690, 800, 730, 760});
        ((ViewGroup) toolRow).getChildAt(3).performClick();
        dispatchStroke(canvas, new float[]{820, 850}, new float[]{1150, 420});
        ((ViewGroup) toolRow).getChildAt(4).performClick();
        dispatchStroke(canvas, new float[]{150, 420}, new float[]{1400, 1720});

        View bar = activity.findViewById(R.id.bar);
        layoutFullScreen(activity.findViewById(R.id.ui));
        // 底部两个横向滚动容器都不应该出现内容溢出（溢出说明窄屏上会被裁切）
        View colorRow = activity.findViewById(R.id.color_row);
        View colorWrap = (View) colorRow.getParent();
        View colorScroll = (View) colorWrap.getParent();
        assertTrue("颜色行溢出", colorWrap.getWidth() <= colorScroll.getWidth());
        // 工具行内容可以比视口宽（HorizontalScrollView 兜底滚动），但面板本身不能超出屏幕
        assertTrue("底部面板不应超出屏幕", bar.getWidth() <= W);
        System.out.println("MEASURE bar=" + bar.getWidth() + " toolRow=" + toolRow.getWidth()
                + " color=" + colorWrap.getWidth() + "/" + colorScroll.getWidth());

        Bitmap bitmap = renderScreen(activity, canvas);
        writePng(bitmap, "02-annotate.png");
    }

    @Test
    @Config(sdk = 34, qualifiers = "w411dp-h891dp-night-xxhdpi")
    public void annotationRendersInNightMode() throws Exception {
        AnnotateActivity activity = Robolectric.buildActivity(AnnotateActivity.class).setup().get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        layoutFullScreen(canvas);
        ViewGroup toolRow = activity.findViewById(R.id.tool_row);
        toolRow.getChildAt(0).performClick();
        dispatchStroke(canvas, new float[]{180, 300, 420, 560, 700},
                new float[]{760, 780, 880, 820, 900});
        toolRow.getChildAt(3).performClick();
        dispatchStroke(canvas, new float[]{240, 250}, new float[]{1200, 520});
        writePng(renderScreen(activity, canvas), "05-annotate-night.png");
    }

    @Test
    public void hideToggleCollapsesToolbarsAndKeepsInk() throws Exception {
        AnnotateActivity activity = Robolectric.buildActivity(AnnotateActivity.class).setup().get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        layoutFullScreen(canvas);

        dispatchStroke(canvas, new float[]{180, 300, 420, 560, 700},
                new float[]{700, 720, 820, 760, 840});
        assertTrue("收起前应有笔迹", canvas.hasInk());

        View bar = activity.findViewById(R.id.bar);
        View rail = activity.findViewById(R.id.rail);
        View chip = activity.findViewById(R.id.chip);
        View toggle = activity.findViewById(R.id.btn_hide);
        assertNotNull("左上角缺少收起按钮", toggle);
        assertEquals(getString(activity, R.string.btn_hide_tools), toggle.getContentDescription());

        toggle.performClick();
        idle(800);
        assertEquals("工具面板应收起", View.GONE, bar.getVisibility());
        assertEquals("右侧操作列应收起", View.GONE, rail.getVisibility());
        assertEquals("顶部胶囊应收起", View.GONE, chip.getVisibility());
        assertTrue("收起后画布内容必须保留", canvas.hasInk());
        assertEquals(getString(activity, R.string.btn_show_tools), toggle.getContentDescription());

        // 收起状态下仍然可以继续书写
        dispatchStroke(canvas, new float[]{200, 320, 460, 600},
                new float[]{980, 1000, 1100, 1040});

        Bitmap clean = renderScreen(activity, canvas);
        writePng(clean, "04-annotate-clean.png");
        int inkPixels = alphaCount(canvas.exportBitmap());
        System.out.println("INK after collapse = " + inkPixels);
        assertTrue("收起后画面里仍应有墨迹", inkPixels > 10);

        toggle.performClick();
        idle(800);
        assertEquals(View.VISIBLE, bar.getVisibility());
        assertEquals(View.VISIBLE, rail.getVisibility());
        assertEquals(View.VISIBLE, chip.getVisibility());
        assertEquals(1f, bar.getAlpha(), 0.01f);
        assertEquals(getString(activity, R.string.btn_hide_tools), toggle.getContentDescription());
    }

    @Test
    public void leavingAnnotateWipesInkAndHistory() throws Exception {
        ActivityController<AnnotateActivity> controller =
                Robolectric.buildActivity(AnnotateActivity.class).setup();
        AnnotateActivity activity = controller.get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        layoutFullScreen(canvas);

        dispatchStroke(canvas, new float[]{200, 320, 460, 600},
                new float[]{700, 720, 820, 760});
        assertTrue("先要有笔迹", canvas.hasInk());

        // 切出应用（回桌面 / 切换应用 / 点完成都会走 pause + stop）
        controller.pause().stop();

        assertFalse("切出后笔迹必须清空", canvas.hasInk());
        assertFalse("撤销历史也要清掉，不能撤回", canvas.undo());
        assertFalse(canvas.redo());

        // 再回来时是一张干净画布，且仍能正常书写
        controller.start().resume();
        layoutFullScreen(canvas);
        dispatchStroke(canvas, new float[]{240, 360, 480},
                new float[]{900, 940, 1000});
        assertTrue("回来后应能继续书写", canvas.hasInk());
    }

    @Test
    public void textToolInsertsTextThroughDialog() throws Exception {
        AnnotateActivity activity = Robolectric.buildActivity(AnnotateActivity.class).setup().get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        layoutFullScreen(canvas);

        ViewGroup toolRow = activity.findViewById(R.id.tool_row);
        toolRow.getChildAt(2).performClick();
        assertEquals("工具条第三格应是文字", Tool.TEXT, canvas.getTool());

        dispatchStroke(canvas, new float[]{300f}, new float[]{900f});

        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("点画布后应弹出输入条", dialog);
        EditText input = dialog.findViewById(R.id.text_input);
        assertNotNull(input);
        input.setText("这里要重点讲");
        dialog.findViewById(R.id.text_ok).performClick();

        assertTrue("确认后应落成一条文字笔迹", canvas.hasInk());
        assertTrue("文字必须画进墨迹层", alphaCount(canvas.exportBitmap()) > 5);

        assertTrue("文字也能撤销", canvas.undo());
        assertFalse(canvas.hasInk());
    }

    @Test
    public void textInputDialogRenders() throws Exception {
        AnnotateActivity activity = Robolectric.buildActivity(AnnotateActivity.class).setup().get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        layoutFullScreen(canvas);

        ((ViewGroup) activity.findViewById(R.id.tool_row)).getChildAt(2).performClick();
        dispatchStroke(canvas, new float[]{420f}, new float[]{1300f});

        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        idle(500); // 等工具栏收起动画结束，预览才和真机一致

        View decor = dialog.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, W, H);

        Bitmap bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        drawMockBackground(c);
        canvas.draw(c);
        activity.findViewById(R.id.ui);
        layoutFullScreen(activity.findViewById(R.id.ui));
        activity.findViewById(R.id.ui).draw(c);
        decor.draw(c);

        writePng(bitmap, "07-text-input.png");
    }

    @Test
    public void textToolIgnoresEmptyInput() throws Exception {
        AnnotateActivity activity = Robolectric.buildActivity(AnnotateActivity.class).setup().get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        layoutFullScreen(canvas);

        ((ViewGroup) activity.findViewById(R.id.tool_row)).getChildAt(2).performClick();
        dispatchStroke(canvas, new float[]{300f}, new float[]{900f});
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);

        dialog.findViewById(R.id.text_ok).performClick();
        assertFalse("空内容不应产生笔迹", canvas.hasInk());
    }

    @Test
    public void eraserRemovesWholeTextBlock() {
        AnnotateActivity activity = Robolectric.buildActivity(AnnotateActivity.class).setup().get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        layoutFullScreen(canvas);

        canvas.addText(200f, 600f, "擦我");
        assertTrue(canvas.hasInk());

        ((ViewGroup) activity.findViewById(R.id.rail)).getChildAt(0).performClick();
        assertEquals(Tool.ERASER, canvas.getTool());
        dispatchStroke(canvas, new float[]{212f}, new float[]{630f});

        assertFalse("擦到文字块应整条删除", canvas.hasInk());
    }

    @Test
    public void textModeRendersPreview() throws Exception {
        AnnotateActivity activity = Robolectric.buildActivity(AnnotateActivity.class).setup().get();
        CanvasView canvas = activity.findViewById(R.id.canvas);
        layoutFullScreen(canvas);
        ViewGroup toolRow = activity.findViewById(R.id.tool_row);

        toolRow.getChildAt(0).performClick();
        dispatchStroke(canvas, new float[]{150, 260, 380, 500},
                new float[]{420, 440, 520, 470});
        toolRow.getChildAt(4).performClick();
        dispatchStroke(canvas, new float[]{700, 700}, new float[]{960, 630});

        canvas.setInkColor(activity.getColor(R.color.ink_white));
        canvas.addText(110f, 1090f, "这个位置要重点讲");
        canvas.setInkColor(activity.getColor(R.color.ink_red));
        canvas.addText(110f, 1440f, "支持多行\n第二行也可以");

        layoutFullScreen(activity.findViewById(R.id.ui));
        writePng(renderScreen(activity, canvas), "06-annotate-text.png");
    }

    @Test
    public void tileStaysInactiveAsOneShotAction() throws Exception {
        android.service.quicksettings.TileService service =
                Robolectric.buildService(InkTileService.class).create().get();
        android.service.quicksettings.Tile tile = service.getQsTile();
        assertNotNull("磁贴应已创建", tile);

        service.onStartListening();
        assertEquals("动作型磁贴不应显示为开启态",
                android.service.quicksettings.Tile.STATE_INACTIVE, tile.getState());

        service.onClick();
        idle(400);
        assertEquals("点击后也不能残留开启态",
                android.service.quicksettings.Tile.STATE_INACTIVE, tile.getState());
    }

    // ---------- 工具方法 ----------

    private static String getString(Activity activity, int res) {
        return activity.getString(res);
    }

    private static void idle(long millis) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    private static void layoutFullScreen(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, W, H);
    }

    private static void dispatchStroke(View target, float[] xs, float[] ys) {
        long t = SystemClock.uptimeMillis();
        target.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, xs[0], ys[0], 0));
        for (int i = 1; i < xs.length; i++) {
            t += 12;
            target.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_MOVE, xs[i], ys[i], 0));
        }
        t += 12;
        target.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_UP, xs[xs.length - 1], ys[ys.length - 1], 0));
    }

    /** 统计非透明像素（隔行/隔列采样），比逐点取像素稳得多。 */
    private static int alphaCount(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] row = new int[w];
        int count = 0;
        for (int y = 0; y < h; y += 2) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x += 2) {
                if (Color.alpha(row[x]) > 10) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 把主面板卡片画在模拟的"下层界面"上，还原真实的浮层观感。 */
    private Bitmap renderCard(Activity activity) {
        View card = activity.findViewById(R.id.scroll);

        int cardWidth = W - dp(activity, 20);
        card.measure(View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.AT_MOST));
        int cardHeight = Math.min(card.getMeasuredHeight(), (int) (H * 0.9));
        card.layout(0, 0, cardWidth, cardHeight);

        Bitmap bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        drawMockBackground(c);

        // 模拟窗口 dim
        c.drawColor(0x75000000);

        int left = dp(activity, 10);
        int top = Math.max(dp(activity, 120), (H - cardHeight) / 2);
        c.save();
        c.translate(left, top);
        card.draw(c);
        c.restore();

        // 顶部状态栏文字，便于确认整屏比例
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(dp(activity, 15));
        c.drawText("12:28", dp(activity, 26), dp(activity, 34), p);
        return bitmap;
    }

    private Bitmap renderScreen(Activity activity, CanvasView canvas) {
        Bitmap bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        drawMockBackground(c);
        canvas.layout(0, 0, W, H);
        canvas.draw(c);

        ViewGroup ui = activity.findViewById(R.id.ui);
        layoutFullScreen(ui);
        ui.draw(c);
        return bitmap;
    }

    private void drawMockBackground(Canvas c) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, W, H,
                new int[]{0xFF1D3B4A, 0xFF274B54, 0xFF1A2B33}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p);

        p.setShader(null);
        p.setColor(0xFF3C5B62);
        c.drawCircle(W * 0.22f, H * 0.22f, W * 0.28f, p);
        p.setColor(0xFF2E4A52);
        c.drawCircle(W * 0.85f, H * 0.42f, W * 0.35f, p);

        // 一些"文字块"，模拟底层应用内容
        p.setColor(0x33FFFFFF);
        for (int i = 0; i < 6; i++) {
            c.drawRoundRect(W * 0.08f, H * (0.16f + i * 0.085f), W * (0.8f - i * 0.05f),
                    H * (0.16f + i * 0.085f) + dp(null, 14), 12, 12, p);
        }
    }

    private static int dp(android.content.Context context, float value) {
        float density = context == null ? 3f : context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private static void writePng(Bitmap bitmap, String name) throws Exception {
        File file = new File(outputDir(), name);
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
        assertTrue(file.exists());
    }
}
