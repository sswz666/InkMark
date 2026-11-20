package com.inkmark.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 主面板：和 ShizukuRunner 一样，用系统 Dialog 主题做成一张浮起来的卡片，
 * 界面外透明，能看到下层（桌面或任意应用），点卡片外即可关闭。
 */
public class MainActivity extends Activity {

    private final ArrayList<ToolCell> toolCells = new ArrayList<>();
    private final ArrayList<DotView> colorDots = new ArrayList<>();
    private final ArrayList<DotView> widthDots = new ArrayList<>();

    private Tool selectedTool;
    private int selectedColor;
    private int widthLevel;

    private static final String[] WIDTH_NAMES = {"细", "中", "粗"};
    private static final float[] WIDTH_DOT_RADIUS = {2.6f, 5f, 8f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        selectedTool = Prefs.tool(this);
        selectedColor = Prefs.color(this);
        widthLevel = Prefs.widthLevel(this);

        setContentView(R.layout.activity_main);
        setFinishOnTouchOutside(true);

        sizeWindow();
        applyHideFromRecents(Prefs.hideRecents(this));

        buildToolGrid();
        buildColors();
        buildWidths();
        bindActions();
    }

    private void sizeWindow() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels - Ui.dp(this, 20);
        int maxHeight = (int) (dm.heightPixels * 0.88f);
        getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        MaxHeightScrollView scroll = findViewById(R.id.scroll);
        scroll.setMaxHeight(maxHeight - Ui.dp(this, 28));
    }

    private void buildToolGrid() {
        GridLayout grid = findViewById(R.id.tool_grid);
        grid.removeAllViews();
        int columns = 4;
        for (int i = 0; i < Tool.ORDER.length; i++) {
            Tool tool = Tool.ORDER[i];
            ToolCell cell = new ToolCell(this);
            cell.setTool(tool);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(i / columns),
                    GridLayout.spec(i % columns, 1f));
            lp.width = 0;
            lp.height = Ui.dp(this, 64);
            int m = Ui.dp(this, 3);
            lp.setMargins(m, m, m, m);
            cell.setLayoutParams(lp);
            cell.setOnClickListener(v -> {
                selectedTool = tool;
                Prefs.setTool(this, tool);
                syncToolCells();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            });
            toolCells.add(cell);
            grid.addView(cell);
        }
        syncToolCells();
    }

    private void syncToolCells() {
        for (ToolCell cell : toolCells) {
            cell.setChecked(cell.getTool() == selectedTool);
        }
    }

    private void buildColors() {
        LinearLayout row = findViewById(R.id.color_row);
        row.removeAllViews();
        colorDots.clear();
        for (int color : Prefs.PALETTE) {
            DotView dot = new DotView(this);
            dot.configure(color, 8f, 12f);
            dot.setRingColor(getColor(R.color.accent));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dp(this, 28), Ui.dp(this, 28));
            lp.setMargins(Ui.dp(this, 1), 0, Ui.dp(this, 1), 0);
            dot.setLayoutParams(lp);
            dot.setOnClickListener(v -> {
                selectedColor = color;
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
            colorDots.get(i).setChecked(Prefs.PALETTE[i] == selectedColor);
        }
    }

    private void buildWidths() {
        LinearLayout row = findViewById(R.id.width_row);
        row.removeAllViews();
        widthDots.clear();
        for (int i = 0; i < WIDTH_DOT_RADIUS.length; i++) {
            final int level = i;
            DotView dot = new DotView(this);
            dot.configure(getColor(R.color.text_primary), WIDTH_DOT_RADIUS[i], 12.5f);
            dot.setRingColor(getColor(R.color.accent));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    Ui.dp(this, 28), Ui.dp(this, 28));
            dot.setLayoutParams(lp);
            dot.setOnClickListener(v -> {
                widthLevel = level;
                Prefs.setWidthLevel(this, level);
                syncWidthDots();
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            });
            widthDots.add(dot);
            row.addView(dot);
        }
        syncWidthDots();
    }

    private void syncWidthDots() {
        for (int i = 0; i < widthDots.size(); i++) {
            widthDots.get(i).configure(getColor(R.color.text_primary), WIDTH_DOT_RADIUS[i], 12.5f);
            widthDots.get(i).setChecked(i == widthLevel);
        }
        TextView label = findViewById(R.id.width_label);
        label.setText(WIDTH_NAMES[widthLevel]);
    }

    private void bindActions() {
        findViewById(R.id.btn_start).setOnClickListener(v -> startAnnotating());

        Switch hide = findViewById(R.id.sw_hide);
        hide.setChecked(Prefs.hideRecents(this));
        hide.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.setHideRecents(this, isChecked);
            applyHideFromRecents(isChecked);
        });
    }

    private void startAnnotating() {
        Prefs.setTool(this, selectedTool);
        Prefs.setColor(this, selectedColor);
        Prefs.setWidthLevel(this, widthLevel);

        Intent intent = new Intent(this, AnnotateActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    private void applyHideFromRecents(boolean hide) {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = am.getAppTasks();
            if (tasks != null && !tasks.isEmpty()) {
                tasks.get(0).setExcludeFromRecents(hide);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从标注页返回后，主面板的选中态可能已变化
        selectedTool = Prefs.tool(this);
        selectedColor = Prefs.color(this);
        widthLevel = Prefs.widthLevel(this);
        syncToolCells();
        syncColorDots();
        syncWidthDots();
    }
}
