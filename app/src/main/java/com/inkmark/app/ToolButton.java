package com.inkmark.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** 标注工具条上的单个工具按钮。 */
public class ToolButton extends FrameLayout {

    private final ImageView icon;
    private Tool tool = Tool.PEN;
    private boolean checked;

    public ToolButton(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        Drawable ripple = getResources().getDrawable(R.drawable.bg_ripple_round, null);
        setForeground(ripple);

        int size = Ui.dp(context, 22);
        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams lp = new LayoutParams(size, size, Gravity.CENTER);
        addView(icon, lp);
        apply();
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
        setContentDescription(getContext().getString(tool.labelRes));
        apply();
    }

    public void setChecked(boolean checked) {
        if (this.checked == checked) {
            return;
        }
        this.checked = checked;
        apply();
    }

    private void apply() {
        Context c = getContext();
        icon.setImageResource(tool.iconRes);
        if (checked) {
            setBackground(Ui.rounded(c, c.getColor(R.color.accent_soft), 14f));
            icon.setColorFilter(c.getColor(R.color.accent));
            animate().scaleX(1f).scaleY(1f).setDuration(120).start();
        } else {
            setBackground(null);
            icon.setColorFilter(c.getColor(R.color.text_secondary));
            animate().scaleX(0.94f).scaleY(0.94f).setDuration(120).start();
        }
    }
}
