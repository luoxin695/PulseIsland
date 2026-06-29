package com.pulseisland.modules;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;
import com.pulseisland.core.IslandLayout;

public class NavigationModule extends IslandLayout {
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint etaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowPath = new Path();
    private float arrowRotation = 0f;
    private String distance = "150m", turnInstruction = "直行", roadName = "中山路";
    private int etaSeconds = 600;
    private boolean isOffRoute = false;
    private CountDownTimer etaTimer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator blinkAnimator;
    private float blinkAlpha = 1.0f;

    public NavigationModule(Context context) {
        super(context);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(0xFF3B82F6);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(dpToPx(9));
        subTextPaint.setStyle(Paint.Style.FILL);
        subTextPaint.setColor(0xFF94A3B8);
        subTextPaint.setTextSize(dpToPx(8));
        etaPaint.setStyle(Paint.Style.FILL);
        etaPaint.setColor(0xFF34C759);
        etaPaint.setTextSize(dpToPx(8));
        roadPaint.setStyle(Paint.Style.FILL);
        roadPaint.setColor(0xFF60A5FA);
        roadPaint.setTextSize(dpToPx(8));
        buildArrowPath();
        initBlinkAnimation();
    }

    private void buildArrowPath() {
        float cx = dpToPx(10), cy = dpToPx(11);
        arrowPath.moveTo(cx, cy - dpToPx(6));
        arrowPath.lineTo(cx + dpToPx(8), cy);
        arrowPath.lineTo(cx, cy + dpToPx(6));
        arrowPath.close();
    }

    private void initBlinkAnimation() {
        blinkAnimator = ValueAnimator.ofFloat(1.0f, 0.2f);
        blinkAnimator.setDuration(500);
        blinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        blinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        blinkAnimator.setInterpolator(new DecelerateInterpolator());
        blinkAnimator.addUpdateListener(a -> { blinkAlpha = (float)a.getAnimatedValue(); if (isOffRoute) invalidate(); });
    }

    private void startEtaCountdown() {
        stopEtaCountdown();
        if (etaSeconds <= 0) return;
        etaTimer = new CountDownTimer(etaSeconds * 1000L, 1000) {
            @Override public void onTick(long m) { etaSeconds = (int)(m / 1000); invalidate(); }
            @Override public void onFinish() { etaSeconds = 0; invalidate(); }
        };
        etaTimer.start();
    }

    private void stopEtaCountdown() { if (etaTimer != null) { etaTimer.cancel(); etaTimer = null; } }
    private String formatEta(int seconds) {
        if (seconds <= 0) return "已到达";
        int m = seconds / 60, s = seconds % 60;
        return m > 0 ? String.format("预计 %d分%d秒", m, s) : String.format("预计 %d秒", s);
    }@Override protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        boolean expanded = isExpanded();
        float cx = dpToPx(10), cy = h / 2f;
        buildArrowPath();
        if (isOffRoute) { arrowPaint.setColor(0xFFEF4444); arrowPaint.setAlpha((int)(255 * blinkAlpha)); }
        else { arrowPaint.setColor(0xFF3B82F6); arrowPaint.setAlpha(255); }
        canvas.save();
        canvas.rotate(arrowRotation, cx, cy);
        canvas.drawPath(arrowPath, arrowPaint);
        canvas.restore();
        float textX = dpToPx(22);
        float topY = expanded ? h * 0.35f : h / 2f - (textPaint.ascent() + textPaint.descent()) / 2;
        textPaint.setTextSize(expanded ? dpToPx(10) : dpToPx(9));
        String main = turnInstruction.isEmpty() ? distance : turnInstruction + " " + distance;
        canvas.drawText(main, textX, topY, textPaint);
        if (expanded) {
            float bottomY = h * 0.75f;
            String roadDisplay = roadName != null ? roadName : "";
            float maxRoadWidth = w * 0.45f;
            if (roadPaint.measureText(roadDisplay) > maxRoadWidth) {
                roadDisplay = roadDisplay.substring(0, Math.min(roadDisplay.length(), 6)) + "…";
            }
            roadPaint.setColor(isOffRoute ? 0xFFEF4444 : 0xFF60A5FA);
            canvas.drawText(roadDisplay, textX, bottomY, roadPaint);
            String etaText = formatEta(etaSeconds);
            float etaX = w - dpToPx(4) - etaPaint.measureText(etaText);
            etaPaint.setColor(0xFF34C759);
            canvas.drawText(etaText, etaX, bottomY, etaPaint);
            if (isOffRoute) {
                subTextPaint.setColor(0xFFEF4444);
                subTextPaint.setTextSize(dpToPx(7));
                String alert = "⛔ 已偏航，正在重新规划";
                float ax = (w - subTextPaint.measureText(alert)) / 2;
                canvas.drawText(alert, ax, h - dpToPx(2), subTextPaint);
            }
        }
    }

    public void setDistance(String d) { distance = d; invalidate(); }
    public void setRotation(float r) { arrowRotation = r; invalidate(); }
    public void setTurnInstruction(String i) { turnInstruction = i; invalidate(); }
    public void setRoadName(String n) { roadName = n; invalidate(); }
    public void setEtaSeconds(int s) { etaSeconds = Math.max(0, s); startEtaCountdown(); invalidate(); }
    public void setOffRoute(boolean off) {
        if (isOffRoute == off) return;
        isOffRoute = off;
        if (off) { if (blinkAnimator != null && !blinkAnimator.isRunning()) blinkAnimator.start(); }
        else { if (blinkAnimator != null && blinkAnimator.isRunning()) blinkAnimator.cancel(); blinkAlpha = 1.0f; }
        invalidate();
    }
    public void updateNavigation(float rot, String dist, String inst, String road, int eta) {
        arrowRotation = rot; distance = dist; turnInstruction = inst; roadName = road; setEtaSeconds(eta);
    }

    @Override protected void onExpandedChanged(boolean e) { super.onExpandedChanged(e); invalidate(); }
    @Override protected void onIslandClick(@NonNull View v) { setExpanded(!isExpanded()); }
    @Override protected void onIslandLongClick(@NonNull View v) { setExpanded(true); }
    @Override protected void onActivated() {
        super.onActivated();
        if (etaSeconds > 0) startEtaCountdown();
        if (isOffRoute && blinkAnimator != null && !blinkAnimator.isRunning()) blinkAnimator.start();
    }
    @Override protected void onDeactivated() {
        super.onDeactivated();
        stopEtaCountdown();
        mainHandler.removeCallbacksAndMessages(null);
        if (blinkAnimator != null && blinkAnimator.isRunning()) blinkAnimator.cancel();
        if (isExpanded()) setExpanded(false);
    }
    @Override public int getPriority() { return 2; }
    @Override public boolean isPersistent() { return false; }
    @Override public long getAutoDismissMs() { return 0; }
}
