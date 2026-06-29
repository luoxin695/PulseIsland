package com.pulseisland.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import com.pulseisland.R;
import com.pulseisland.utils.DeviceAdapter;

public class LockScreenOverlayActivity extends Activity {
    private FrameLayout container;
    private LockView lockView;
    private Handler handler = new Handler();
    private boolean isUnlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        container = new FrameLayout(this);
        container.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        lockView = new LockView(this);
        FrameLayout.LayoutParams lockParams = new FrameLayout.LayoutParams(
                dpToPx(28), dpToPx(28)
        );
        lockParams.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        lockParams.setMarginEnd(dpToPx(8));
        container.addView(lockView, lockParams);

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        wm.addView(container, params);

        startUnlockPolling();
    }

    private void startUnlockPolling() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isUnlocked) return;
                KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                if (km != null && !km.isKeyguardLocked()) {
                    isUnlocked = true;
                    lockView.playUnlockAnimation();
                    handler.postDelayed(() -> finish(), 500);
                    return;
                }
                handler.postDelayed(this, 50);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (container != null) {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (wm != null) wm.removeView(container);
        }
        handler.removeCallbacksAndMessages(null);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private static class LockView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path lockPath = new Path();
        private boolean isUnlocking = false;

        public LockView(Context context) {
            super(context);
            lockPath.moveTo(0, 6);
            lockPath.lineTo(0, 18);
            lockPath.arcTo(0, 18, 14, 32, 180, -180, false);
            lockPath.lineTo(14, 6);
            lockPath.moveTo(2, 6);
            lockPath.arcTo(2, -2, 12, 8, 180, 180, false);
            lockPath.lineTo(12, 6);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (isUnlocking) return;
            int w = getWidth(), h = getHeight();
            float scale = 0.6f;
            canvas.save();
            canvas.translate(w / 2f, h / 2f);
            canvas.scale(scale, scale);
            paint.setColor(0xFF94A3B8);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f);
            canvas.drawPath(lockPath, paint);
            canvas.restore();
        }

        public void playUnlockAnimation() {
            if (isUnlocking) return;
            isUnlocking = true;
            animate()
                .translationY(-dpToPx(12))
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(400)
                .setInterpolator(new OvershootInter
