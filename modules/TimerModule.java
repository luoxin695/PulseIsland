package com.pulseisland.modules;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.CountDownTimer;
import android.view.View;

import androidx.annotation.NonNull;

import com.pulseisland.core.IslandLayout;

public class TimerModule extends IslandLayout {

    // ===== 绘制工具 =====
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ===== 计时数据 =====
    private String time = "02:34";
    private long remainingMillis = 300000; // 默认 5 分钟
    private boolean isTimerRunning = false;

    // ===== 计时器 =====
    private CountDownTimer timer;

    public TimerModule(Context context) {
        super(context);
        initPaints();
    }

    private void initPaints() {
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setColor(0xFFFFFFFF);
        iconPaint.setStrokeWidth(dpToPx(1.2f));

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(dpToPx(10));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // 时钟图标
        float iconX = dpToPx(10);
        float cy = h / 2f;
        float radius = dpToPx(6);
        canvas.drawCircle(iconX, cy, radius, iconPaint);
        canvas.drawLine(iconX, cy, iconX + radius * 0.6f, cy - radius * 0.4f, iconPaint);
        canvas.drawLine(iconX, cy, iconX + radius * 0.6f, cy + radius * 0.4f, iconPaint);

        // 时间文字
        boolean expanded = isExpanded();
        textPaint.setTextSize(expanded ? dpToPx(16) : dpToPx(10));

        float textX = dpToPx(20);
        float textY = h / 2f - (textPaint.ascent() + textPaint.descent()) / 2;
        canvas.drawText(time, textX, textY, textPaint);
    }

    public void startTimer(long millis) {
        stopTimer();
        this.remainingMillis = millis;
        this.time = formatTime(millis);

        timer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                time = formatTime(millisUntilFinished);
                isTimerRunning = true;
                invalidate();
            }

            @Override
            public void onFinish() {
                time = "00:00";
                remainingMillis = 0;
                isTimerRunning = false;
                invalidate();
            }
        };
        timer.start();
        isTimerRunning = true;
        invalidate();
    }

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isTimerRunning = false;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }

    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000);
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    @Override
    protected void onExpandedChanged(boolean expanded) {
        super.onExpandedChanged(expanded);
        invalidate();
    }

    @Override
    protected void onActivated() {
        super.onActivated();
        if (remainingMillis > 0 && !isTimerRunning && timer == null) {
            startTimer(remainingMillis);
        }
        invalidate();
    }

    @Override
    protected void onDeactivated() {
        super.onDeactivated();
        if (timer != null) {
            timer.cancel();
            timer = null;
            isTimerRunning = false;
        }
    }

    @Override
    protected void onIslandClick(@NonNull View v) {
        setExpanded(!isExpanded());
    }

    @Override
    protected void onIslandLongClick(@NonNull View v) {
        setExpanded(true);
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public long getAutoDismissMs() {
        return 0;
    }
                  }
