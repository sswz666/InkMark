package com.inkmark.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** 圆点选择器：颜色点与笔触点共用，选中时外面加一圈细环。 */
public class DotView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int fillColor = 0xFF1B1B1F;
    private int ringColor = 0xFF5B6BD8;
    private float dotRadiusDp = 8.5f;
    private float ringRadiusDp = 12f;
    private float ringWidthDp = 1.8f;
    private boolean checked;

    public DotView(Context context) {
        super(context);
        setClickable(true);
        setForeground(getResources().getDrawable(R.drawable.bg_ripple_round, null));
    }

    public void configure(int fillColor, float dotRadiusDp, float ringRadiusDp) {
        this.fillColor = fillColor;
        this.dotRadiusDp = dotRadiusDp;
        this.ringRadiusDp = ringRadiusDp;
        invalidate();
    }

    public void setRingColor(int ringColor) {
        this.ringColor = ringColor;
        invalidate();
    }

    public void setChecked(boolean checked) {
        if (this.checked == checked) {
            return;
        }
        this.checked = checked;
        float target = checked ? 1f : 0.82f;
        animate().scaleX(target).scaleY(target).setDuration(140).start();
        invalidate();
    }

    public boolean isCheckedValue() {
        return checked;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float d = getResources().getDisplayMetrics().density;

        if (checked) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(ringColor);
            paint.setStrokeWidth(ringWidthDp * d);
            canvas.drawCircle(cx, cy, ringRadiusDp * d - ringWidthDp * d * 0.5f, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        canvas.drawCircle(cx, cy, dotRadiusDp * d, paint);

        // 极亮 / 极暗的点在面板上需要一层薄轮廓，深色与浅色主题都不会糊掉
        int r = Color.red(fillColor);
        int g = Color.green(fillColor);
        int b = Color.blue(fillColor);
        float lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
        if (lum < 0.12f || lum > 0.9f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(lum < 0.12f ? 0x55FFFFFF : 0x22000000);
            paint.setStrokeWidth(d * 1f);
            canvas.drawCircle(cx, cy, dotRadiusDp * d, paint);
        }
    }
}
