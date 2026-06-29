package com.pulseisland.modules;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.pulseisland.core.IslandLayout;

public class CallModule extends IslandLayout {

    // ===== 绘制工具 =====
    private final Paint avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint endCallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint endCallIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ===== 通话数据 =====
    private String contactName = "来电";
    private String duration = "00:00";
    private boolean isConnected = false;
    private boolean isRinging = false;

    // ===== 计时器 =====
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int callSeconds = 0;
    private Runnable timerRunnable;

    // ===== 系统服务 =====
    private TelephonyManager telephonyManager;

    // ===== 回调接口 =====
    private OnCallActionListener callActionListener;

    public interface OnCallActionListener {
        void onAnswerCall();
        void onEndCall();
        void onCallDismiss();
    }

    public CallModule(Context context) {
        super(context);
        initPaints();
        registerTelephonyListener();
    }

    private void initPaints() {
        avatarPaint.setStyle(Paint.Style.FILL);
        avatarPaint.setColor(0xFF6366F1);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextSize(dpToPx(9));

        timerPaint.setStyle(Paint.Style.FILL);
        timerPaint.setColor(0xFFFFFFFF);
        timerPaint.setTextSize(dpToPx(10));

        endCallPaint.setStyle(Paint.Style.FILL);
        endCallPaint.setColor(0xFFFF3B30);

        endCallIconPaint.setStyle(Paint.Style.STROKE);
        endCallIconPaint.setColor(0xFFFFFFFF);
        endCallIconPaint.setStrokeWidth(dpToPx(1.5f));
        endCallIconPaint.setAntiAlias(true);
    }

    // ===== 电话监听 =====
    private void registerTelephonyListener() {
        telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                    getContext().getMainExecutor(),
                    new TelephonyCallback() {
                        @Override
                        public void onCallStateChanged(int state, String phoneNumber) {
                            handleCallState(state, phoneNumber);
                        }
                    }
            );
        } else {
            // 低版本兼容（使用广播接收器，此处省略）
        }
    }

    private void handleCallState(int state, String phoneNumber) {
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isRinging = true;
                isConnected = false;
                contactName = !TextUtils.isEmpty(phoneNumber) ? phoneNumber : "未知号码";
                callSeconds = 0;
                duration = "00:00";
                stopTimer();
                invalidate();
                if (callActionListener != null) {
                    callActionListener.onAnswerCall();
                }
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
                isRinging = false;
                isConnected = true;
                startTimer();
                invalidate();
                break;

            case TelephonyManager.CALL_STATE_IDLE:
                isRinging = false;
                isConnected = false;
                stopTimer();
                callSeconds = 0;
                duration = "00:00";
                invalidate();
                if (callActionListener != null) {
                    callActionListener.onCallDismiss();
                }
                break;
        }
    }

    // ===== 通话计时器 =====
    private void startTimer() {
        stopTimer();
        callSeconds = 0;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                callSeconds++;
                int mins = callSeconds / 60;
                int secs = callSeconds % 60;
                duration = String.format("%02d:%02d", mins, secs);
                invalidate();
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
    // ===== 生命周期 =====
    @Override
    protected void onActivated() {
        super.onActivated();
        if (isConnected) {
            startTimer();
        }
        invalidate();
    }

    @Override
    protected void onDeactivated() {
        super.onDeactivated();
        stopTimer();
    }

    // ===== 绘制 =====
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // 1. 头像
        float avatarSize = dpToPx(16);
        float cx = dpToPx(3) + avatarSize / 2;
        float cy = h / 2f;
        canvas.drawCircle(cx, cy, avatarSize / 2, avatarPaint);

        // 2. 联系人姓名
        textPaint.setTextSize(dpToPx(9));
        textPaint.setColor(0xFFE2E8F0);
        float nameX = dpToPx(24);
        float nameY = h / 2f - (textPaint.ascent() + textPaint.descent()) / 2;
        canvas.drawText(contactName, nameX, nameY, textPaint);

        // 3. 计时器
        timerPaint.setColor(isConnected ? 0xFF34C759 : (isRinging ? 0xFFFF9500 : 0xFF94A3B8));
        String display = isConnected ? duration : (isRinging ? "响铃中" : "待接听");
        float timerX = w - dpToPx(6) - timerPaint.measureText(display);
        float timerY = h / 2f - (timerPaint.ascent() + timerPaint.descent()) / 2;
        canvas.drawText(display, timerX, timerY, timerPaint);

        // 4. 展开模式：挂断按钮
        if (isExpanded()) {
            float btnCx = w - dpToPx(18);
            float btnCy = h / 2f;
            float radius = dpToPx(10);
            canvas.drawCircle(btnCx, btnCy, radius, endCallPaint);

            // 挂断图标（电话听筒向下）
            float half = dpToPx(6);
            Path path = new Path();
            path.moveTo(btnCx - half * 0.4f, btnCy - half * 0.3f);
            path.cubicTo(btnCx - half * 0.5f, btnCy + half * 0.2f,
                    btnCx + half * 0.5f, btnCy + half * 0.2f,
                    btnCx + half * 0.4f, btnCy - half * 0.3f);
            path.lineTo(btnCx + half * 0.2f, btnCy - half * 0.3f);
            path.cubicTo(btnCx + half * 0.2f, btnCy - half * 0.1f,
                    btnCx - half * 0.2f, btnCy - half * 0.1f,
                    btnCx - half * 0.2f, btnCy - half * 0.3f);
            path.close();
            canvas.drawPath(path, endCallIconPaint);
        }
    }

    // ===== 交互 =====
    @Override
    protected void onIslandClick(@NonNull View v) {
        setExpanded(!isExpanded());
    }

    @Override
    protected void onIslandLongClick(@NonNull View v) {
        if (isRinging && callActionListener != null) {
            callActionListener.onAnswerCall();
        } else if (isConnected && callActionListener != null) {
            callActionListener.onEndCall();
        }
    }

    // ===== 必须实现的抽象方法 =====
    @Override
    public int getPriority() {
        return 100;
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
