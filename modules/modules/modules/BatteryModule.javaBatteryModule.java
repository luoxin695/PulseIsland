package com.pulseisland.modules;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import androidx.annotation.NonNull;
import com.pulseisland.core.IslandLayout;

public class BatteryModule extends IslandLayout {
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lowLevelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int level = 72;
    private boolean isCharging = false;

    public BatteryModule(Context context) {
        super(context);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setColor(0xFFFFFFFF);
        framePaint.setStrokeWidth(dpToPx(1.2f));
        levelPaint.setStyle(Paint.Style.FILL);
        levelPaint.setColor(0xFF34C759);
        lowLevelPaint.setStyle(Paint.Style.FILL);
        lowLevelPaint.setColor(0xFFFF9500);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFF94A3B8);
        textPaint.setTextSize(dpToPx(8));
    }

    @Override protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        boolean expanded = isExpanded();
        float cx = w / 2f, cy = h / 2f, half = dpToPx(7);
        float bw = half * 1.6f, bh = half * 0.8f, bx = cx - bw / 2f, by = cy - bh / 2f;
        RectF rect = new RectF(bx, by, bx + bw, by + bh);
        canvas.drawRoundRect(rect, half * 0.15f, half * 0.15f, framePaint);
        float headW = half * 0.15f, headH = bh * 0.35f;
        canvas.drawRect(bx + bw, cy - headH / 2f, bx + bw + headW, cy + headH / 2f, framePaint);
        float inner = half * 0.08f;
        float lvW = (bw - inner * 2) * Math.min(1f, Math.max(0f, level / 100f));
        RectF lvRect = new RectF(bx + inner, by + inner, bx + inner + lvW, by + bh - inner);
        Paint fillPaint;
        if (level <= 15) { fillPaint = lowLevelPaint; lowLevelPaint.setColor(0xFFFF3B30); }
        else if (level <= 30) { fillPaint = lowLevelPaint; lowLevelPaint.setColor(0xFFFF9500); }
        else { fillPaint = levelPaint; levelPaint.setColor(0xFF34C759); }
        canvas.drawRoundRect(lvRect, half * 0.05f, half * 0.05f, fillPaint);
        if (isCharging) {
            textPaint.setColor(0xFFFFD700);
            textPaint.setTextSize(dpToPx(8));
            if (expanded && w > dpToPx(100)) {
                canvas.drawText("⚡", w - dpToPx(12), h / 2 + dpToPx(4), textPaint);
            } else {
                canvas.drawText("⚡", bx + dpToPx(2), cy + dpToPx(4), textPaint);
            }
        }
        if (expanded) {
            String detail = level + "%" + (isCharging ? " 充电中" : "");
            textPaint.setColor(0xFFE2E8F0);
            textPaint.setTextSize(dpToPx(8));
            float x = w / 2f - textPaint.measureText(detail) / 2;
            canvas.drawText(detail, x, h - dpToPx(4), textPaint);
            if (level <= 15) {
                textPaint.setColor(0xFFFF3B30);
                textPaint.setTextSize(dpToPx(7));
                canvas.drawText("⚠ 电量过低", dpToPx(4), h - dpToPx(4), textPaint);
            }
        }
    }

    public void setLevel(int l) { level = Math.max(0, Math.min(100, l)); invalidate(); }
    public void setCharging(boolean c) { isCharging = c; invalidate(); }
    public int getLevel() { return level; }
    public boolean isCharging() { return isCharging; }

    @Override protected void onExpandedChanged(boolean e) { super.onExpandedChanged(e); invalidate(); }
    @Override protected void onIslandClick(@NonNull View v) { setExpanded(!isExpanded()); }
    @Override protected void onIslandLongClick(@NonNull View v) { setExpanded(true); }
    @Override protected void onActivated() { super.onActivated(); }
    @Override protected void onDeactivated() { super.onDeactivated(); if (isExpanded()) setExpanded(false); }
    @Override public int getPriority() { return 1; }
    @Override public boolean isPersistent() { return false; }
    @Override public long getAutoDismissMs() { return 0; }
}                               }
