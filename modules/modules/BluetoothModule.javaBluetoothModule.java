package com.pulseisland.modules;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import androidx.annotation.NonNull;

import com.pulseisland.core.IslandLayout;

public class BluetoothModule extends IslandLayout {
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String deviceName = "";
    private int batteryLevel = -1;
    private boolean isConnected = false;

    public BluetoothModule(Context context) {
        super(context);
        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(0xFFFFFFFF);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(dpToPx(9));
    }

    @Override protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();

        if (!isConnected) {
            textPaint.setColor(0xFF64748B);
            float x = w / 2f - dpToPx(16);
            float y = h / 2f - (textPaint.ascent() + textPaint.descent()) / 2;
            canvas.drawText("无设备", x, y, textPaint);
            return;
        }

        float half = dpToPx(7), cx = dpToPx(10), cy = h / 2f;
        Path path = new Path();
        path.moveTo(cx + half * 0.25f, cy - half * 0.85f);
        path.lineTo(cx + half * 0.75f, cy - half * 0.5f);
        path.cubicTo(cx + half * 0.9f, cy - half * 0.3f, cx + half * 0.9f, cy + half * 0.3f, cx + half * 0.75f, cy + half * 0.5f);
        path.lineTo(cx + half * 0.25f, cy + half * 0.85f);
        path.moveTo(cx + half * 0.15f, cy - half * 0.35f);
        path.lineTo(cx + half * 0.85f, cy - half * 0.35f);
        path.moveTo(cx + half * 0.15f, cy + half * 0.35f);
        path.lineTo(cx + half * 0.85f, cy + half * 0.35f);
        canvas.drawPath(path, iconPaint);

        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(dpToPx(9));
        float nameX = dpToPx(20);
        float nameY = h / 2f - (textPaint.ascent() + textPaint.descent()) / 2;
        String display = deviceName.length() > 14 ? deviceName.substring(0, 14) + "…" : deviceName;
        canvas.drawText(display, nameX, nameY, textPaint);

        if (batteryLevel >= 0) {
            textPaint.setColor(0xFF94A3B8);
            textPaint.setTextSize(dpToPx(8));
            String batt = batteryLevel + "%";
            float battX = w - dpToPx(6) - textPaint.measureText(batt);
            canvas.drawText(batt, battX, nameY, textPaint);
        }

        if (isExpanded()) {
            textPaint.setColor(0xFF94A3B8);
            textPaint.setTextSize(dpToPx(8));
            String status = "已连接" + (batteryLevel >= 0 ? " · 电量" + batteryLevel + "%" : "");
            canvas.drawText(status, dpToPx(4), h - dpToPx(4), textPaint);
        }
    }

    public void updateDevice(BluetoothDevice device, int battery) {
        if (device != null) {
            deviceName = device.getName() != null ? device.getName() : "未知设备";
            isConnected = true;
        } else {
            isConnected = false;
            deviceName = "";
        }
        batteryLevel = battery;
        invalidate();
    }

    @Override protected void onIslandClick(@NonNull View v) { setExpanded(!isExpanded()); }
    @Override protected void onIslandLongClick(@NonNull View v) { setExpanded(true); }
    @Override protected void onActivated() { super.onActivated(); }
    @Override protected void onDeactivated() { super.onDeactivated(); if (isExpanded()) setExpanded(false); }
    @Override public int getPriority() { return 2; }
    @Override public boolean isPersistent() { return false; }
    @Override public long getAutoDismissMs() { return 0; }
}                       }
