package com.inkmark.app;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * 标注层：全屏半透明的 Activity，界面外完全透明，下层应用清晰可见。
 * 不申请悬浮窗 / 无障碍权限，靠 windowIsTranslucent 的透明窗口实现"覆盖"。
 */
public class AnnotateActivity extends Activity implements CanvasView.Listener {

    private static final int REQ_STORAGE = 7;

    /** 系统权限弹窗期间 Activity 可能被 stop，此时不能清空画布。 */
    private boolean awaitingPermission;

    private CanvasView canvas;
    private TextView chipText;
    private View chip;
    private View bar;
    private View rail;
    private View hideButton;
    private ImageView hideIcon;
    private View undoButton;
    private View redoButton;
    private ToolButton eraserButton;

    /** 工具栏是否已收起（收起后只剩左上角的倒 V，画面保持原样）。 */
    private boolean toolsHidden;

    private final ArrayList<ToolButton> toolButtons = new ArrayList<>();
    private final ArrayList<DotView> colorDots = new ArrayList<>();

    private Tool tool;
    private int inkColor;
    private int widthLevel;

    private static final float[] WIDTH_DOT_RADIUS = {2.6f, 5f, 8f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindow();
        setContentView(R.layout.activity_annotate);

        canvas = findViewById(R.id.canvas);
        canvas.setListener(this);
        chipText = findViewById(R.id.chip_text);
        chip = findViewById(R.id.chip);
        bar = findViewById(R.id.bar);
        rail = findViewById(R.id.rail);

        tool = Prefs.tool(this);
        inkColor = Prefs.color(this);
        widthLevel = Prefs.widthLevel(this);

        canvas.setInkColor(inkColor);
        canvas.setWidthLevel(widthLevel);
        canvas.setTool(tool);

        buildColorRow();
        buildWidthRow();
        buildToolRow();
        buildRail();
        setupHideToggle();
        applyInsets();
        updateChip();
        updateHistoryButtons(false, false);
        ensureHiddenFromRecents();
    }

    /** 双保险：即便某些 ROM 忽略清单里的 excludeFromRecents，也把本应用任务移出多任务列表。 */
    private void ensureHiddenFromRecents() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            for (ActivityManager.AppTask task : am.getAppTasks()) {
                task.setExcludeFromRecents(true);
            }
        } catch (Throwable ignored) {
        }
    }

    // ================= 收起 / 展开工具栏 =================

    private void setupHideToggle() {
        hideButton = findViewById(R.id.btn_hide);
        hideIcon = findViewById(R.id.btn_hide_icon);
        hideIcon.setColorFilter(getColor(R.color.text_secondary));
        hideButton.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            setToolsHidden(!toolsHidden);
        });
    }

    /**
     * 收起时把底部工具面板、右侧操作列和顶部状态胶囊一起淡出，
     * 画布上的笔迹完整保留，仍处于标注模式，可以直接继续书写；
     * 只剩左上角的倒 V 按钮作为"展开"入口。
     */
    private void setToolsHidden(boolean hidden) {
        if (toolsHidden == hidden) {
            return;
        }
        toolsHidden = hidden;

        if (hidden) {
            slideOut(bar, 0f, Ui.dp(this, 26));
            slideOut(rail, Ui.dp(this, 26), 0f);
            slideOut(chip, 0f, -Ui.dp(this, 14));
        } else {
            slideIn(bar, 0f, Ui.dp(this, 26));
            slideIn(rail, Ui.dp(this, 26), 0f);
            slideIn(chip, 0f, -Ui.dp(this, 14));
        }

        hideIcon.animate().cancel();
        hideIcon.animate().rotationX(hidden ? 180f : 0f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f))
                .start();
        hideButton.setContentDescription(getString(
                hidden ? R.string.btn_show_tools : R.string.btn_hide_tools));
    }

    private void slideOut(View view, float endX, float endY) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(1f);
        view.animate()
                .alpha(0f)
                .translationX(endX)
                .translationY(endY)
                .setDuration(170)
                .setInterpolator(new android.view.animation.AccelerateInterpolator(1.4f))
                .withEndAction(() -> {
                    view.setAlpha(0f);
                    view.setVisibility(toolsHidden ? View.GONE : View.VISIBLE);
                })
                .start();
    }

    private void slideIn(View view, float startX, float startY) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationX(startX);
        view.setTranslationY(startY);
        view.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .setDuration(210)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f))
                .start();
    }

    private void setupWindow() {
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        setFinishOnTouchOutside(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    private void applyInsets() {
        final View ui = findViewById(R.id.ui);
        final int baseLeft = ui.getPaddingLeft();
        final int baseTop = ui.getPaddingTop();
        final int baseRight = ui.getPaddingRight();
        final int baseBottom = ui.getPaddingBottom();

        ui.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            int left;
            int right;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = bars.top;
                bottom = bars.bottom;
                left = bars.left;
                right = bars.right;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft();
                right = insets.getSystemWindowInsetRight();
            }
            v.setPadding(baseLeft + left, baseTop + top, baseRight + right, baseBottom + bottom);
            return insets;
        });
        ui.requestApplyInsets();
    }

    // ================= 工具行 =================

    private void buildToolRow() {
        LinearLayout row = findViewById(R.id.tool_row);
        row.removeAllViews();
        toolButtons.clear();
        // 按钮尺寸按屏宽自适应：宽屏最大 46dp，窄屏自动缩小，保证 8 个工具不用滚动就能看全
        int usable = getResources().getDisplayMetrics().widthPixels
                - Ui.dp(this, 10) * 2      // 面板左右外边距
                - Ui.dp(this, 6) * 2       // 面板内边距
                - Ui.dp(this, 4) * 2;      // 行内水平内边距
        int size = Math.min(Ui.dp(this, 44), usable / Tool.BAR_ORDER.length);
        for (Tool t : Tool.BAR_ORDER) {
            ToolButton button = new ToolButton(this);
            button.setTool(t);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            button.setLayoutParams(lp);
            button.setOnClickListener(v -> selectTool(t));
            toolButtons.add(button);
            row.addView(button);
        }
        syncToolButtons();
    }

    private void selectTool(Tool t) {
        tool = t;
        canvas.setTool(t);
        Prefs.setTool(this, t);
        syncToolButtons();
        updateChip();
    }

    private void syncToolButtons() {
        for (ToolButton button : toolButtons) {
            button.setChecked(button.getTool() == tool);
        }
        if (eraserButton != null) {
            eraserButton.setChecked(tool == Tool.ERASER);
        }
    }

    // ================= 颜色 / 笔触 =================

    private void buildColorRow() {
        LinearLayout row = findViewById(R.id.color_row);
        row.removeAllViews();
        colorDots.clear();
        for (int color : Prefs.PALETTE) {
            DotView dot = new DotView(this);
            dot.configure(color, 8f, 12f);
            dot.setRingColor(getColor(R.color.accent));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dp(this, 28), Ui.dp(this, 28));
            dot.setLayoutParams(lp);
            dot.setOnClickListener(v -> {
                inkColor = color;
                canvas.setInkColor(color);
                Prefs.setColor(this, color);
                syncColorDots();
                syncWidthDots();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            });
            colorDots.add(dot);
            row.addView(dot);
        }
        syncColorDots();
    }

    private void syncColorDots() {
        for (int i = 0; i < colorDots.size(); i++) {
            colorDots.get(i).setChecked(Prefs.PALETTE[i] == inkColor);
        }
    }

    private void buildWidthRow() {
        LinearLayout row = findViewById(R.id.width_row);
        row.removeAllViews();
        for (int i = 0; i < WIDTH_DOT_RADIUS.length; i++) {
            final int level = i;
            DotView dot = new DotView(this);
            dot.configure(getColor(R.color.text_primary), WIDTH_DOT_RADIUS[i], 13f);
            dot.setRingColor(getColor(R.color.accent));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dp(this, 28), Ui.dp(this, 28));
            dot.setLayoutParams(lp);
            dot.setOnClickListener(v -> {
                widthLevel = level;
                canvas.setWidthLevel(level);
                Prefs.setWidthLevel(this, level);
                syncWidthDots();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            });
            dot.setTag(level);
            row.addView(dot);
        }
        syncWidthDots();
    }

    private void syncWidthDots() {
        LinearLayout row = findViewById(R.id.width_row);
        for (int i = 0; i < row.getChildCount(); i++) {
            DotView dot = (DotView) row.getChildAt(i);
            dot.configure(getColor(R.color.text_primary), WIDTH_DOT_RADIUS[i], 13f);
            dot.setChecked(i == widthLevel);
        }
    }

    // ================= 右侧操作列 =================

    private void buildRail() {
        LinearLayout rail = findViewById(R.id.rail);
        rail.removeAllViews();

        eraserButton = new ToolButton(this);
        eraserButton.setTool(Tool.ERASER);
        eraserButton.setOnClickListener(v -> selectTool(Tool.ERASER));
        addRailView(rail, eraserButton, 42, 0);

        undoButton = action(R.drawable.ic_undo, getString(R.string.btn_undo), () -> {
            if (canvas.undo()) {
                canvas.invalidate();
            }
        });
        addRailView(rail, undoButton, 42, 2);

        redoButton = action(R.drawable.ic_redo, getString(R.string.btn_redo), () -> {
            if (canvas.redo()) {
                canvas.invalidate();
            }
        });
        addRailView(rail, redoButton, 42, 0);

        View clear = action(R.drawable.ic_trash, getString(R.string.btn_clear), () -> {
            if (canvas.hasInk()) {
                canvas.clear();
                Toast.makeText(this, R.string.toast_cleared, Toast.LENGTH_SHORT).show();
            }
        });
        addRailView(rail, clear, 42, 2);

        View save = action(R.drawable.ic_save, getString(R.string.btn_save), this::savePng);
        addRailView(rail, save, 42, 0);

        View done = action(R.drawable.ic_check, getString(R.string.btn_done), this::finish);
        FrameLayout doneBox = (FrameLayout) done;
        doneBox.setBackground(getResources().getDrawable(R.drawable.bg_round_accent, null));
        ImageView doneIcon = (ImageView) doneBox.getChildAt(0);
        doneIcon.setColorFilter(Color.WHITE);
        addRailView(rail, doneBox, 46, 4);
    }

    private void addRailView(LinearLayout rail, View view, int sizeDp, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                Ui.dp(this, sizeDp), Ui.dp(this, sizeDp));
        lp.topMargin = Ui.dp(this, topMarginDp);
        rail.addView(view, lp);
    }

    private View action(int iconRes, String desc, Runnable action) {
        FrameLayout box = new FrameLayout(this);
        box.setForeground(getResources().getDrawable(R.drawable.bg_ripple_round, null));
        box.setClickable(true);
        box.setFocusable(true);
        box.setContentDescription(desc);
        box.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            action.run();
        });

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getColor(R.color.text_secondary));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                Ui.dp(this, 20), Ui.dp(this, 20), Gravity.CENTER);
        box.addView(icon, lp);
        return box;
    }

    // ================= 文字输入 =================

    @Override
    public void onTextRequested(float x, float y) {
        showTextInput(x, y);
    }

    /**
     * 文字模式：点画布后弹出底部输入条，确认才落成一条墨迹（可撤销、可擦除、可导出）。
     * 用 Dialog 承载输入法，键盘弹起时窗口 resize，输入条自动贴在键盘上方，
     * 也不会牵动透明 Activity 的画布尺寸。
     */
    private void showTextInput(final float x, final float y) {
        if (isFinishing()) {
            return;
        }
        // 输入时先把底部工具面板和右侧操作列收起来，避免和输入条叠在一起
        final boolean wasHidden = toolsHidden;
        if (!wasHidden) {
            setToolsHidden(true);
        }

        final Dialog dialog = new Dialog(this, R.style.Theme_InkMark_Input);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_text);
        dialog.setCanceledOnTouchOutside(true);

        final EditText input = dialog.findViewById(R.id.text_input);
        View dot = dialog.findViewById(R.id.text_dot);
        dot.setBackground(Ui.circle(this, inkColor));

        ImageView cancelIcon = dialog.findViewById(R.id.text_cancel_icon);
        cancelIcon.setColorFilter(getColor(R.color.text_secondary));

        // 点卡片外（整屏窗口的空白处）取消
        dialog.findViewById(R.id.text_root).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.text_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.text_ok).setOnClickListener(v -> {
            String content = input.getText().toString();
            if (content.trim().isEmpty()) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                Toast.makeText(this, R.string.text_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            canvas.addText(x, y, content);
            dialog.dismiss();
        });

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
                window.setGravity(Gravity.BOTTOM);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
            input.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        dialog.setOnDismissListener(d -> {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            }
            if (!wasHidden) {
                setToolsHidden(false);
            }
        });
        dialog.show();
    }

    // ================= 状态 =================

    private void updateChip() {
        chipText.setText(getString(R.string.chip_annotating) + " · " + getString(tool.labelRes));
    }

    private void updateHistoryButtons(boolean canUndo, boolean canRedo) {
        if (undoButton != null) {
            undoButton.setEnabled(canUndo);
            undoButton.setAlpha(canUndo ? 1f : 0.3f);
        }
        if (redoButton != null) {
            redoButton.setEnabled(canRedo);
            redoButton.setAlpha(canRedo ? 1f : 0.3f);
        }
    }

    @Override
    public void onHistoryChanged(boolean canUndo, boolean canRedo) {
        updateHistoryButtons(canUndo, canRedo);
    }

    // ================= 保存 =================

    private void savePng() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            awaitingPermission = true;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        doSave();
    }

    private void doSave() {
        if (!canvas.hasInk()) {
            return;
        }
        Bitmap bitmap = canvas.exportBitmap();
        boolean saved = PngExporter.save(this, bitmap);
        Toast.makeText(this, saved ? R.string.toast_saved : R.string.toast_save_fail,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE) {
            awaitingPermission = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doSave();
            } else {
                Toast.makeText(this, R.string.toast_perm_needed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ================= 生命周期 =================

    /**
     * 切出即擦除：回到桌面、切换应用、点「完成」都会走到 onStop，
     * 此时把笔迹连同撤销历史一起丢弃，下次进入是一张干净的画布。
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (awaitingPermission) {
            // 正在等系统权限弹窗（保存图片），回来还要继续画，不能清
            return;
        }
        wipeInk();
    }

    private void wipeInk() {
        if (canvas == null) {
            return;
        }
        canvas.wipe();
        updateHistoryButtons(false, false);
    }
}
