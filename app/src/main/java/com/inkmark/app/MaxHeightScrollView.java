package com.inkmark.app;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/** 限制最大高度的 ScrollView，保证弹出卡片不会撑出屏幕。 */
public class MaxHeightScrollView extends ScrollView {

    private int maxHeight = Integer.MAX_VALUE;

    public MaxHeightScrollView(Context context) {
        super(context);
    }

    public MaxHeightScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int size = MeasureSpec.getSize(heightMeasureSpec);
        if (mode != MeasureSpec.UNSPECIFIED && size > maxHeight) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
        } else if (mode == MeasureSpec.UNSPECIFIED) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
