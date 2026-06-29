package com.pulseisland.modules;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;
import com.pulseisland.core.IslandLayout;

public class FaceIDModule extends IslandLayout {
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path checkPath = new Path();
    private boolean isSuccess = false;
    private float checkProgress = 0f;
    private ValueAnimator checkAnimator;
    private ValueAnimator failAnimator;

    public FaceIDModule(Context context) {
        super(context);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setColor(0xFFFFFFFF);
        circlePaint.setStrokeWidth(dpToPx(2f));
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setColor(0xFF34C759);
        checkPaint.setStrokeWidth(dpToPx(3f));
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFF94A3B8);
        textPaint.setTextSize(dpToPx(9));
    }

    @Override protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        boolean expanded = isExpanded();
        if (isSuccess) {
            float cx = w / 2f, cy = h / 2f, radius = dpToPx(7);
            circlePaint.setColor(0xFF34C759);
            canvas.drawCircle(cx, cy, radius, circlePaint);
            checkPath.reset();
            checkPath.moveTo(cx - radius * 0.5f, cy);
            checkPath.lineTo(cx - radius * 0.1f, cy + radius * 0.5f * checkProgress);
            checkPath.lineTo(cx + radius * 0.6f, cy - radius * 0.5f * checkProgress);
            canvas.drawPath(checkPath, checkPaint);
        } else if (checkProgress < 0.5f && checkProgress > 0f) {
            float cx = w / 2f, cy = h / 2f, radius = dpToPx(7);
            circlePaint.setColor(0xFFFF3B30);
            canvas.drawCircle(cx, cy, radius, circlePaint);
            float xLen = radius * 0.5f * checkProgress * 2;
            canvas.drawLine(cx - xLen, cy - xLen, cx + xLen, cy + xLen, checkPaint);
            canvas.drawLine(cx + xLen, cy - xLen, cx - xLen, cy + xLen, checkPaint);
        } else {
            float tx = w / 2f - dpToPx(18);
            float ty = h / 2f - (textPaint.ascent() + textPaint.descent()) / 2;
            canvas.drawText("面容 ID", tx, ty, textPaint);
        }
        if (expanded) {
            textPaint.setColor(isSuccess ? 0xFF34C759 : 0xFFFF3B30);
            textPaint.setTextSize(dpToPx(10));
            String status = isSuccess ? "✓ 验证通过" : "✗ 验证失败";
            float x = w / 2f - textPaint.measureText(status) / 2;
            canvas.drawText(status, x, h - dpToPx(4), textPaint);
        }
    }

    public void setSuccess(boolean success) {
        if (this.isSuccess == success && checkProgress == (success ? 1f : 0f)) return;
        this.isSuccess = success;
        cancelAnimations();
        if (success) {
            checkAnimator = ValueAnimator.ofFloat(0f, 1f);
            checkAnimator.setDuration(400);
            checkAnimator.setInterpolator(new DecelerateInterpolator());
            checkAnimator.addUpdateListener(a -> { checkProgress = (float)a.getAnimatedValue(); invalidate(); });
            checkAnimator.start();
        } else {
            checkProgress = 0.5f;
            invalidate();
            failAnimator = ValueAnimator.ofFloat(0.5f, 0f);
            failAnimator.setDuration(300);
            failAnimator.setInterpolator(new DecelerateInterpolator());
            failAnimator.addUpdateListener(a -> { checkProgress = (float)a.getAnimatedValue(); invalidate(); });
            failAnimator.start();
        }
    }

    private void cancelAnimations() {
        if (checkAnimator != null && checkAnimator.isRunning()) { checkAnimator.cancel(); checkAnimator = null; }
        if (failAnimator != null && failAnimator.isRunning()) { failAnimator.cancel(); failAnimator = null; }
    }

    @Override protected void onExpandedChanged(boolean e) { super.onExpandedChanged(e); invalidate(); }
    @Override protected void onIslandClick(@NonNull View v) { setExpanded(!isExpanded()); }
    @Override protected void onIslandLongClick(@NonNull View v) { setExpanded(true); }
    @Override protected void onActivated() { super.onActivated(); }
    @Override protected void onDeactivated() { super.onDeactivated(); cancelAnimations(); if (isExpanded()) setExpanded(false); }
    @Override public int getPriority() { return 2; }
    @Override public boolean isPersistent() { return false; }
    @Override public long getAutoDismissMs() { return isSuccess
