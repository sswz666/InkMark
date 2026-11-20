package com.inkmark.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 主面板里的工具格：图标 + 名称。 */
public class ToolCell extends LinearLayout {

    private final ImageView icon;
    private final TextView label;
    private Tool tool = Tool.PEN;
    private boolean checked;

    public ToolCell(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setClickable(true);
        setFocusable(true);
        setForeground(getResources().getDrawable(R.drawable.bg_ripple_round, null));
        setPadding(0, Ui.dp(context, 8), 0, Ui.dp(context, 8));

        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(icon, new LayoutParams(Ui.dp(context, 21), Ui.dp(context, 21)));

        label = new TextView(context);
        label.setTextSize(11f);
        label.setIncludeFontPadding(false);
        label.setGravity(Gravity.CENTER);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(context, 5);
        addView(label, lp);

        apply();
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
        label.setText(tool.labelRes);
        apply();
    }

    public void setChecked(boolean checked) {
        if (this.checked == checked) {
            return;
        }
        this.checked = checked;
        apply();
        if (checked) {
            animate().scaleX(1f).scaleY(1f).setDuration(120).start();
        }
    }

    private void apply() {
        Context c = getContext();
        icon.setImageResource(tool.iconRes);
        if (checked) {
            setBackground(Ui.rounded(c, c.getColor(R.color.accent_soft), 16f));
            icon.setColorFilter(c.getColor(R.color.accent));
            label.setTextColor(c.getColor(R.color.accent));
            label.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            setBackground(null);
            icon.setColorFilter(c.getColor(R.color.text_primary));
            label.setTextColor(c.getColor(R.color.text_secondary));
            label.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
}
