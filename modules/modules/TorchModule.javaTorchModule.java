package com.pulseisland.modules;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

import com.pulseisland.core.IslandLayout;

public class TorchModule extends IslandLayout {
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint beamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bottomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean isOn = false;
    private float glowRadius = 0f;
    private float beamWidth = 0f;

    private CameraManager cameraManager;
    private String cameraId;

    public TorchModule(Context context) {
        super(context);
        glowPaint.setStyle(Paint.Style.FILL);
        beamPaint.setStyle(Paint.Style.STROKE);
        beamPaint.setStrokeWidth(dpToPx(2.5f));
        beamPaint.setStrokeCap(Paint.Cap.ROUND);
        topPaint.setStyle(Paint.Style.FILL);
        topPaint.setColor(0xFFFFFFFF);
        bottomPaint.setStyle(Paint.Style.FILL);
        bottomPaint.setColor(0xFFFFFFFF);
        dividerPaint.setStyle(Paint.Style.FILL);
        dividerPaint.setColor(0xFF555555);

        cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void toggleTorch() {
        try {
            if (cameraManager == null || cameraId == null) return;
            isOn = !isOn;
            cameraManager.setTorchMode(cameraId, isOn);
            if (isOn) {
                startGlowAnimation(0f, dpToPx(28));
                startBeamAnimation(0f, 1f);
                setExpanded(true);
            } else {
                startGlowAnimation(glowRadius, 0f);
                startBeamAnimation(beamWidth, 0f);
                postDelayed(() -> { if (isExpanded()) setExpanded(false); }, 400);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startGlowAnimation(float from, float to) {
        ValueAnimator anim = ValueAnimator.ofFloat(from, to);
        anim.setDuration(350);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            glowRadius = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    private void startBeamAnimation(float from, float to) {
        ValueAnimator anim = ValueAnimator.ofFloat(from, to);
        anim.setDuration(350);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            beamWidth = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;

        if (isOn && isExpanded()) {
            RadialGradient glow = new RadialGradient(
                    cx, cy - dpToPx(2), glowRadius,
                    new int[]{0x40FFD700, 0x10FFD700, 0x00FFD700},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP
            );
            glowPaint.setShader(glow);
            canvas.drawCircle(cx, cy - dpToPx(2), glowRadius, glowPaint);

            beamPaint.setColor(0xFFFFD700);
            beamPaint.setAlpha((int) (180 * beamWidth));
            float beamW = dpToPx(14) * beamWidth;
            canvas.drawLine(cx - beamW / 2, cy + dpToPx(8), cx + beamW / 2, cy + dpToPx(8), beamPaint);
        }

        int iconColor = isOn ? 0xFFFFFFFF : 0xFF787878;
        topPaint.setColor(iconColor);
        bottomPaint.setColor(iconColor);

        float scale = 0.9f;
        float iconW = dpToPx(7.67f * scale);
        float iconH = dpToPx(22f * scale);
        float iconX = cx - iconW / 2;
        float iconY = cy - iconH / 2 - dpToPx(1);
        float radius = dpToPx(3.83f * scale);

        Path topPath = new Path();
        topPath.addRoundRect(
                new RectF(iconX, iconY, iconX + iconW, iconY + dpToPx(7.33f * scale)),
                new float[]{radius, radius, radius, radius, 0, 0, 0, 0},
                Path.Direction.CW
        );
        canvas.drawPath(topPath, topPaint);

        float divY = iconY + dpToPx(7.33f * scale);
        canvas.drawRect(ic
