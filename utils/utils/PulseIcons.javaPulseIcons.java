package com.pulseisland.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

public class PulseIcons {

    public static void drawBluetooth(Canvas canvas, float x, float y, float size, Paint paint, boolean glowing) {
        float half = size / 2f;
        if (glowing) {
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            RadialGradient rg = new RadialGradient(x, y, half * 1.5f,
                    new int[]{0x4081D4FA, 0x00FFFFFF},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP);
            glow.setShader(rg);
            canvas.drawCircle(x, y, half * 1.5f, glow);
        }

        paint.setColor(0xFFFFFFFF);
        Path path = new Path();
        path.moveTo(x + half * 0.25f, y - half * 0.85f);
        path.lineTo(x + half * 0.75f, y - half * 0.5f);
        path.cubicTo(x + half * 0.9f, y - half * 0.3f, x + half * 0.9f, y + half * 0.3f, x + half * 0.75f, y + half * 0.5f);
        path.lineTo(x + half * 0.25f, y + half * 0.85f);
        path.moveTo(x + half * 0.15f, y - half * 0.35f);
        path.lineTo(x + half * 0.85f, y - half * 0.35f);
        path.moveTo(x + half * 0.15f, y + half * 0.35f);
        path.lineTo(x + half * 0.85f, y + half * 0.35f);
        canvas.drawPath(path, paint);
    }

    public static void drawTorch(Canvas canvas, float x, float y, float scale,
                                  Paint topPaint, Paint bottomPaint, Paint dividerPaint) {
        float w = 7.67f * scale;
        float topH = 7.33f * scale;
        float radius = 3.83f * scale;

        Path topPath = new Path();
        topPath.addRoundRect(
                new RectF(x, y, x + w, y + topH),
                new float[]{radius, radius, radius, radius, 0, 0, 0, 0},
                Path.Direction.CW
        );
        canvas.drawPath(topPath, topPaint);

        float divY = y + topH;
        canvas.drawRect(x, divY, x + w, divY + 0.33f * scale, dividerPaint);

        float botY = y + topH + 0.33f * scale;
        float botH = 14.33f * scale;
        Path bottomPath = new Path();
        bottomPath.addRoundRect(
                new RectF(x, botY, x + w, botY + botH),
                new float[]{0, 0, 0, 0, radius, radius, radius, radius},
                Path.Direction.CW
        );
        canvas.drawPath(bottomPath, bottomPaint);
    }

    public static void drawBattery(Canvas canvas, float x, float y, float size,
                                    Paint framePaint, Paint levelPaint, float level) {
        float half = size / 2f;
        float bw = half * 1.6f;
        float bh = half * 0.8f;
        float bx = x;
        float by = y;

        RectF rect = new RectF(bx, by, bx + bw, by + bh);
        canvas.drawRoundRect(rect, half * 0.15f, half * 0.15f, framePaint);

        float headW = half * 0.15f;
        float headH = bh * 0.35f;
        canvas.drawRect(bx + bw, by + bh / 2f - headH / 2f, bx + bw + headW, by + bh / 2f + headH / 2f, framePaint);

        float inner = half * 0.08f;
        float lvW = (bw - inner * 2) * Math.min(1f, level);
        RectF lvRect = new RectF(bx + inner, by + inner, bx + inner + lvW, by + bh - inner);
        canvas.drawRoundRect(lvRect, half * 0.05f, half * 0.05f, levelPaint);
    }
}
