package com.inkmark.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 标注画布：全部笔画绘制在透明图层上，下层界面透过来。
 */
public class CanvasView extends View {

    public interface Listener {
        void onHistoryChanged(boolean canUndo, boolean canRedo);
    }

    private static final float[] WIDTHS_DP = {2.5f, 5.5f, 10f};
    private static final long LASER_LIFE_MS = 680L;

    private final History<Ink> history = new History<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final ArrayList<LaserDot> laser = new ArrayList<>();

    private Ink current;
    private Tool tool = Tool.PEN;
    private int inkColor = 0xFF1B1B1F;
    private int widthLevel = 1;
    private float lastX;
    private float lastY;
    private boolean changedInGesture;
    private Listener listener;

    private static final class LaserDot {
        final float x;
        final float y;
        final float r;
        final long born;

        LaserDot(float x, float y, float r) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.born = SystemClock.uptimeMillis();
        }
    }

    public CanvasView(Context context) {
        this(context, null);
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setWillNotDraw(false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
        if (tool == Tool.LASER) {
            laser.clear();
        }
        invalidate();
    }

    public int getInkColor() {
        return inkColor;
    }

    public void setInkColor(int color) {
        this.inkColor = color;
    }

    public int getWidthLevel() {
        return widthLevel;
    }

    public void setWidthLevel(int level) {
        this.widthLevel = Math.max(0, Math.min(WIDTHS_DP.length - 1, level));
    }

    private float strokeWidth() {
        return dp(WIDTHS_DP[widthLevel]) * tool.widthFactor;
    }

    public boolean hasInk() {
        return !history.state().isEmpty();
    }

    public boolean undo() {
        boolean ok = history.undo();
        if (ok) {
            notifyChanged();
            invalidate();
        }
        return ok;
    }

    public boolean redo() {
        boolean ok = history.redo();
        if (ok) {
            notifyChanged();
            invalidate();
        }
        return ok;
    }

    public void clear() {
        if (history.state().isEmpty()) {
            return;
        }
        history.snapshot();
        history.state().clear();
        laser.clear();
        notifyChanged();
        invalidate();
    }

    /** 彻底丢弃全部笔迹与撤销历史，用于"切出即擦除"。 */
    public void wipe() {
        history.clear();
        laser.clear();
        invalidate();
    }

    /** 导出透明背景的标注图层。 */
    public Bitmap exportBitmap() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        for (Ink ink : history.state()) {
            drawInk(canvas, ink);
        }
        return bitmap;
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onHistoryChanged(history.canUndo(), history.canRedo());
        }
    }

    // ================= 触摸 =================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                changedInGesture = false;
                lastX = x;
                lastY = y;
                if (tool == Tool.LASER) {
                    laser.clear();
                    addLaser(x, y);
                    invalidate();
                } else {
                    history.snapshot();
                    if (tool == Tool.ERASER) {
                        eraseAt(x, y);
                    } else {
                        current = new Ink(tool, inkColor, strokeWidth());
                        current.add(x, y);
                        invalidate();
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                handleMove(event, x, y);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                finishGesture();
                return true;

            default:
                return true;
        }
    }

    private void handleMove(MotionEvent event, float x, float y) {
        int historySize = event.getHistorySize();
        for (int i = 0; i < historySize; i++) {
            float hx = event.getHistoricalX(0, i);
            float hy = event.getHistoricalY(0, i);
            step(hx, hy);
        }
        step(x, y);
        invalidate();
    }

    private void step(float x, float y) {
        if (tool == Tool.LASER) {
            addLaser(x, y);
            return;
        }
        if (tool == Tool.ERASER) {
            eraseAt(x, y);
            return;
        }
        if (current == null) {
            return;
        }
        float min = dp(1.2f);
        if (Math.hypot(x - lastX, y - lastY) < min) {
            return;
        }
        lastX = x;
        lastY = y;
        current.add(x, y);
        changedInGesture = true;
    }

    private void finishGesture() {
        if (tool == Tool.LASER) {
            return;
        }
        if (current != null) {
            float min = dp(3f);
            if (current.isDegenerate(min)) {
                changedInGesture = false;
            } else {
                history.state().add(current);
                changedInGesture = true;
            }
            current = null;
        }
        if (!changedInGesture) {
            history.discardLastSnapshot();
        }
        invalidate();
        notifyChanged();
    }

    // ================= 橡皮 =================

    private void eraseAt(float x, float y) {
        float radius = strokeWidth() + dp(6f);
        List<Ink> inks = history.state();
        boolean removed = false;
        for (Iterator<Ink> it = inks.iterator(); it.hasNext(); ) {
            if (hit(it.next(), x, y, radius)) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            changedInGesture = true;
            invalidate();
        }
    }

    private boolean hit(Ink ink, float x, float y, float r) {
        float tolerance = r + ink.width * 0.5f;
        switch (ink.tool) {
            case PEN:
            case MARKER: {
                List<PointF> pts = ink.points;
                if (pts.size() == 1) {
                    return Math.hypot(x - pts.get(0).x, y - pts.get(0).y) <= tolerance;
                }
                for (int i = 1; i < pts.size(); i++) {
                    PointF a = pts.get(i - 1);
                    PointF b = pts.get(i);
                    if (Geom.distToSegment(x, y, a.x, a.y, b.x, b.y) <= tolerance) {
                        return true;
                    }
                }
                return false;
            }
            case LINE:
            case ARROW:
                return Geom.distToSegment(x, y, ink.sx(), ink.sy(), ink.ex(), ink.ey()) <= tolerance;
            case RECT:
                return Geom.distToRectEdge(x, y, ink.sx(), ink.sy(), ink.ex(), ink.ey()) <= tolerance;
            case ELLIPSE: {
                float cx = (ink.sx() + ink.ex()) * 0.5f;
                float cy = (ink.sy() + ink.ey()) * 0.5f;
                float rx = Math.abs(ink.ex() - ink.sx()) * 0.5f;
                float ry = Math.abs(ink.ey() - ink.sy()) * 0.5f;
                return Geom.distToEllipseOutline(x, y, cx, cy, rx, ry) <= tolerance;
            }
            default:
                return false;
        }
    }

    // ================= 激光 =================

    private void addLaser(float x, float y) {
        float min = dp(2f);
        if (!laser.isEmpty()) {
            LaserDot last = laser.get(laser.size() - 1);
            if (Math.hypot(x - last.x, y - last.y) < min) {
                return;
            }
        }
        laser.add(new LaserDot(x, y, strokeWidth() * 1.1f));
        postInvalidateOnAnimation();
    }

    // ================= 绘制 =================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Ink ink : history.state()) {
            drawInk(canvas, ink);
        }
        if (current != null) {
            drawInk(canvas, current);
        }
        if (!laser.isEmpty()) {
            drawLaser(canvas);
        }
    }

    private void drawInk(Canvas canvas, Ink ink) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(ink.color);
        paint.setAlpha(255);
        paint.setStrokeWidth(ink.width);

        switch (ink.tool) {
            case PEN:
                drawFreehand(canvas, ink);
                break;
            case MARKER:
                paint.setAlpha(96);
                drawFreehand(canvas, ink);
                break;
            case LINE:
                canvas.drawLine(ink.sx(), ink.sy(), ink.ex(), ink.ey(), paint);
                break;
            case ARROW:
                drawArrow(canvas, ink);
                break;
            case RECT:
                rect.set(Math.min(ink.sx(), ink.ex()), Math.min(ink.sy(), ink.ey()),
                        Math.max(ink.sx(), ink.ex()), Math.max(ink.sy(), ink.ey()));
                canvas.drawRect(rect, paint);
                break;
            case ELLIPSE:
                rect.set(Math.min(ink.sx(), ink.ex()), Math.min(ink.sy(), ink.ey()),
                        Math.max(ink.sx(), ink.ex()), Math.max(ink.sy(), ink.ey()));
                canvas.drawOval(rect, paint);
                break;
            default:
                break;
        }
    }

    private void drawFreehand(Canvas canvas, Ink ink) {
        List<PointF> pts = ink.points;
        if (pts.isEmpty()) {
            return;
        }
        if (pts.size() == 1) {
            PointF p = pts.get(0);
            int alpha = paint.getAlpha();
            Paint.Style style = paint.getStyle();
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(p.x, p.y, ink.width * 0.5f, paint);
            paint.setStyle(style);
            paint.setAlpha(alpha);
            return;
        }
        path.reset();
        path.moveTo(pts.get(0).x, pts.get(0).y);
        for (int i = 1; i < pts.size() - 1; i++) {
            PointF p = pts.get(i);
            PointF next = pts.get(i + 1);
            path.quadTo(p.x, p.y, (p.x + next.x) * 0.5f, (p.y + next.y) * 0.5f);
        }
        PointF last = pts.get(pts.size() - 1);
        path.lineTo(last.x, last.y);
        canvas.drawPath(path, paint);
    }

    private void drawArrow(Canvas canvas, Ink ink) {
        float x1 = ink.sx();
        float y1 = ink.sy();
        float x2 = ink.ex();
        float y2 = ink.ey();
        canvas.drawLine(x1, y1, x2, y2, paint);

        double angle = Math.atan2(y2 - y1, x2 - x1);
        float head = Math.max(dp(14f), ink.width * 3.2f);
        float spread = 0.44f;
        float ax = x2 - (float) (Math.cos(angle - spread) * head);
        float ay = y2 - (float) (Math.sin(angle - spread) * head);
        float bx = x2 - (float) (Math.cos(angle + spread) * head);
        float by = y2 - (float) (Math.sin(angle + spread) * head);
        path.reset();
        path.moveTo(ax, ay);
        path.lineTo(x2, y2);
        path.lineTo(bx, by);
        canvas.drawPath(path, paint);
    }

    private void drawLaser(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        for (Iterator<LaserDot> it = laser.iterator(); it.hasNext(); ) {
            LaserDot d = it.next();
            long age = now - d.born;
            if (age > LASER_LIFE_MS) {
                it.remove();
                continue;
            }
            float k = 1f - (float) age / LASER_LIFE_MS;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(inkColor);
            paint.setAlpha((int) (k * 215));
            canvas.drawCircle(d.x, d.y, d.r * (0.55f + 0.45f * k), paint);
        }
        if (!laser.isEmpty()) {
            // 激光轨迹需要逐帧淡出
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        laser.clear();
    }
}
