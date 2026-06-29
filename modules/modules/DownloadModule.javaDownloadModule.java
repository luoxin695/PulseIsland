package com.pulseisland.modules;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;

import com.pulseisland.core.IslandLayout;

public class DownloadModule extends IslandLayout {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String appName = "应用";
    private int progress = 0;
    private boolean isActive = false;
    private Runnable hideRunnable;

    public DownloadModule(Context context) {
        super(context);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(0xFF334155);
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(0xFF38BDF8);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(dpToPx(8));
    }

    @Override protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();

        if (!isActive) {
            textPaint.setColor(0xFF64748B);
            float x = w / 2f - dpToPx(12);
            float y = h / 2f - (textPaint.ascent() + textPaint.descent()) / 2;
            canvas.drawText("无下载", x, y, textPaint);
            return;
        }

        textPaint.setTextSize(dpToPx(9));
        textPaint.setColor(0xFFFFFFFF);
        canvas.drawText("⬇", dpToPx(4), h / 2f + dpToPx(4), textPaint);

        textPaint.setTextSize(dpToPx(8));
        float nameX = dpToPx(16);
        float nameY = h / 2f - (textPaint.ascent() + textPaint.descent()) / 2;
        canvas.drawText(appName, nameX, nameY, textPaint);

        textPaint.setColor(0xFF38BDF8);
        String percent = progress + "%";
        float pX = w - dpToPx(6) - textPaint.measureText(percent);
        canvas.drawText(percent, pX, nameY, textPaint);

        textPaint.setColor(0xFFFFFFFF);
        float barX = dpToPx(4);
        float barY = h - dpToPx(3);
        float barW = w - dpToPx(8);
        float barH = dpToPx(1.5f);
        canvas.drawRoundRect(barX, barY - barH / 2, barX + barW, barY + barH / 2,
                dpToPx(1), dpToPx(1), barPaint);
        float prog = barW * progress / 100f;
        canvas.drawRoundRect(barX, barY - barH / 2, barX + prog, barY + barH / 2,
                dpToPx(1), dpToPx(1), progressPaint);
    }

    public void update(String name, int prog) {
        if (hideRunnable != null) {
            handler.removeCallbacks(hideRunnable);
            hideRunnable = null;
        }
        appName = name;
        progress = Math.max(0, Math.min(100, prog));
        isActive = true;
        setVisibility(VISIBLE);
        invalidate();
    }

    public void complete() {
        isActive = false;
        invalidate();
        hideRunnable = () -> {
            setVisibility(GONE);
            hideRunnable = null;
        };
        handler.postDelayed(hideRunnable, 3000);
    }

    @Override protected void onIslandClick(@NonNull View v) { setExpanded(!isExpanded()); }
    @Override protected void onIslandLongClick(@NonNull View v) { setExpanded(true); }
    @Override protected void onActivated() { super.onActivated(); }
    @Override protected void onDeactivated() {
        super.onDeactivated();
        if (hideRunnable != null) {
            handler.removeCallbacks(hideRunnable);
            hideRunnable = null;
        }
        if (isExpanded()) setExpanded(false);
    }
    @Override public int getPriority() { return 2; }
    @Override public boolean isPersistent() { return false; }
    @Override public long getAutoDismissMs() { return 0; }
}
