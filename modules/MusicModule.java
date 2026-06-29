package com.pulseisland.modules;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

import com.pulseisland.core.IslandLayout;

import java.util.List;

public class MusicModule extends IslandLayout {

    // ===== 绘制工具 =====
    private final Paint coverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ===== 媒体数据 =====
    private MediaController mediaController;
    private MediaController.Callback mediaCallback;
    private String currentTitle = "未在播放";
    private String currentArtist = "";
    private boolean isPlaying = false;
    private boolean isMuted = false;
    private int currentProgress = 0;      // 0~100

    // ===== 动画 =====
    private float[] waveHeights = {0f, 0f, 0f, 0f, 0f};
    private android.animation.ValueAnimator waveAnim;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ===== 状态 =====
    private boolean isWaveAnimRunning = false;

    public MusicModule(Context context) {
        super(context);
        initPaints();
        setupMediaSession();
        startWaveAnimation();
        updateMediaInfo();
    }

    private void initPaints() {
        coverPaint.setStyle(Paint.Style.FILL);
        coverPaint.setColor(0xFF4F46E5);

        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeCap(Paint.Cap.ROUND);
        wavePaint.setStrokeWidth(dpToPx(1.2f));

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xFF94A3B8);
        textPaint.setTextSize(dpToPx(9));

        progressBgPaint.setStyle(Paint.Style.FILL);
        progressBgPaint.setColor(0xFF334155);

        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(0xFF38BDF8);
    }

    // ===== 媒体会话管理 =====
    private void setupMediaSession() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        MediaSessionManager manager = (MediaSessionManager) getContext()
                .getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (manager == null) return;
        try {
            List<MediaController> controllers = manager.getActiveSessions(null);
            if (!controllers.isEmpty()) {
                setMediaController(controllers.get(0));
            }
            // 监听新会话的出现（切换音乐 App 时更新）
            manager.addOnActiveSessionsChangedListener(sessionInfos -> {
                if (sessionInfos != null && !sessionInfos.isEmpty()) {
                    MediaController newController = new MediaController(getContext(), sessionInfos.get(0));
                    setMediaController(newController);
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMediaController(MediaController controller) {
        if (mediaController != null && mediaCallback != null) {
            mediaController.unregisterCallback(mediaCallback);
        }
        this.mediaController = controller;
        if (controller != null) {
            mediaCallback = new MediaController.Callback() {
                @Override
                public void onPlaybackStateChanged(android.media.session.PlaybackState state) {
                    updateMediaInfo();
                }

                @Override
                public void onMetadataChanged(android.media.MediaMetadata metadata) {
                    updateMediaInfo();
                }
            };
            controller.registerCallback(mediaCallback);
        }
        updateMediaInfo();
    }

    private void updateMediaInfo() {
        if (mediaController == null) {
            currentTitle = "未在播放";
            currentArtist = "";
            isPlaying = false;
            currentProgress = 0;
            invalidate();
            return;
        }
        android.media.session.PlaybackState state = mediaController.getPlaybackState();
        isPlaying = state != null && state.getState() == android.media.session.PlaybackState.STATE_PLAYING;
        if (isPlaying && state != null) {
            long position = state.getPosition();
            long duration = mediaController.getMetadata() != null ?
                    mediaController.getMetadata().getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) : 0;
            currentProgress = duration > 0 ? (int) (100 * position / duration) : 0;
        } else// ===== 波形动画 =====
    private void startWaveAnimation() {
        if (waveAnim != null && waveAnim.isRunning()) return;
        waveAnim = android.animation.ValueAnimator.ofFloat(0f, 1f);
        waveAnim.setDuration(600);
        waveAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        waveAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        waveAnim.setInterpolator(new DecelerateInterpolator());
        waveAnim.addUpdateListener(anim -> {
            float progress = (float) anim.getAnimatedValue();
            for (int i = 0; i < waveHeights.length; i++) {
                float centerFactor = 1f - Math.abs(i - 2) * 0.2f;
                waveHeights[i] = 0.2f + 0.8f * progress * centerFactor;
            }
            invalidate();
        });
        waveAnim.start();
        isWaveAnimRunning = true;
    }

    private void stopWaveAnimation() {
        if (waveAnim != null) {
            waveAnim.cancel();
            waveAnim.removeAllUpdateListeners();
            waveAnim = null;
        }
        isWaveAnimRunning = false;
    }

    // ===== 展开状态变化 =====
    @Override
    protected void onExpandedChanged(boolean expanded) {
        super.onExpandedChanged(expanded);
        invalidate();
    }

    // ===== 绘制 =====
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // 1. 封面
        float coverSize = dpToPx(15);
        RectF coverRect = new RectF(dpToPx(3), (h - coverSize) / 2,
                dpToPx(3) + coverSize, (h + coverSize) / 2);
        canvas.drawRoundRect(coverRect, dpToPx(8), dpToPx(8), coverPaint);

        // 2. 静音状态检查
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            isMuted = (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT);
        }

        // 3. 波形
        if (isPlaying && !isMuted) {
            float startX = dpToPx(22);
            float centerY = h / 2f;
            float maxHeight = dpToPx(8);
            float gap = dpToPx(2.5f);
            wavePaint.setColor(0xFFFFFFFF);
            wavePaint.setStrokeWidth(dpToPx(1.2f));

            for (int i = 0; i < 5; i++) {
                float x = startX + i * gap;
                float height = maxHeight * waveHeights[i];
                canvas.drawLine(x, centerY - height, x, centerY + height, wavePaint);
            }
        }

        // 4. 展开模式
        if (isExpanded()) {
            // 进度条
            float barY = h - dpToPx(6);
            float barLeft = dpToPx(22);
            float barRight = w - dpToPx(4);
            canvas.drawRoundRect(barLeft, barY, barRight, barY + dpToPx(2),
                    dpToPx(1), dpToPx(1), progressBgPaint);
            float progressW = (barRight - barLeft) * (currentProgress / 100f);
            canvas.drawRoundRect(barLeft, barY, barLeft + progressW, barY + dpToPx(2),
                    dpToPx(1), dpToPx(1), progressPaint);

            // 歌名
            String displayText = currentTitle;
            if (currentArtist != null && !currentArtist.isEmpty()) {
                displayText += " · " + currentArtist;
            }
            textPaint.setColor(0xFFE2E8F0);
            textPaint.setTextSize(dpToPx(9));
            float textX = dpToPx(22) + (isPlaying && !isMuted ? dpToPx(20) : 0);
            float textY = h / 2f + dpToPx(3);
            canvas.drawText(displayText, textX, textY, textPaint);
        }
    }

    // ===== 生命周期 =====
    @Override
    protected void onActivated() {
        super.onActivated();
        if (!isWaveAnimRunning) {
            startWaveAnimation();
        }
        setupMediaSession();
        updateMediaInfo();
    }

    @Override
    protected void onDeactivated() {
        super.onDeactivated();
        stopWaveAnimation();
        if (mediaController != null && mediaCallback != null) {
            mediaController.unregisterCallback(mediaCallback);
            mediaCallback = null;
        }
        mediaController = null;
        mainHandler.removeCallbacksAndMessages(null);
    }

    // ===== 交互 =====
    @Override
    protected void onIslandClick(@NonNull View v) {
        if (mediaController != null) {
            if (isPlaying) {
                mediaController.getTransportControls().pause();
            } else {
                mediaController.getTransportControls().play();
            }
        }
    }

    @Override
    protected void onIslandLongClick(@NonNull View v) {
        setExpanded(!isExpanded());
    }

    // ===== 必须实现的抽象方法 =====
    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public long getAutoDismissMs() {
        return 0;
    }
              }// ===== 波形动画 =====
    private void startWaveAnimation() {
        if (waveAnim != null && waveAnim.isRunning()) return;
        waveAnim = android.animation.ValueAnimator.ofFloat(0f, 1f);
        waveAnim.setDuration(600);
        waveAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        waveAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        waveAnim.setInterpolator(new DecelerateInterpolator());
        waveAnim.addUpdateListener(anim -> {
            float progress = (float) anim.getAnimatedValue();
            for (int i = 0; i < waveHeights.length; i++) {
                float centerFactor = 1f - Math.abs(i - 2) * 0.2f;
                waveHeights[i] = 0.2f + 0.8f * progress * centerFactor;
            }
            invalidate();
        });
        waveAnim.start();
        isWaveAnimRunning = true;
    }

    private void stopWaveAnimation() {
        if (waveAnim != null) {
            waveAnim.cancel();
            waveAnim.removeAllUpdateListeners();
            waveAnim = null;
        }
        isWaveAnimRunning = false;
    }

    // ===== 展开状态变化 =====
    @Override
    protected void onExpandedChanged(boolean expanded) {
        super.onExpandedChanged(expanded);
        invalidate();
    }

    // ===== 绘制 =====
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        // 1. 封面
        float coverSize = dpToPx(15);
        RectF coverRect = new RectF(dpToPx(3), (h - coverSize) / 2,
                dpToPx(3) + coverSize, (h + coverSize) / 2);
        canvas.drawRoundRect(coverRect, dpToPx(8), dpToPx(8), coverPaint);

        // 2. 静音状态检查
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            isMuted = (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT);
        }

        // 3. 波形
        if (isPlaying && !isMuted) {
            float startX = dpToPx(22);
            float centerY = h / 2f;
            float maxHeight = dpToPx(8);
            float gap = dpToPx(2.5f);
            wavePaint.setColor(0xFFFFFFFF);
            wavePaint.setStrokeWidth(dpToPx(1.2f));

            for (int i = 0; i < 5; i++) {
                float x = startX + i * gap;
                float height = maxHeight * waveHeights[i];
                canvas.drawLine(x, centerY - height, x, centerY + height, wavePaint);
            }
        }

        // 4. 展开模式
        if (isExpanded()) {
            // 进度条
            float barY = h - dpToPx(6);
            float barLeft = dpToPx(22);
            float barRight = w - dpToPx(4);
            canvas.drawRoundRect(barLeft, barY, barRight, barY + dpToPx(2),
                    dpToPx(1), dpToPx(1), progressBgPaint);
            float progressW = (barRight - barLeft) * (currentProgress / 100f);
            canvas.drawRoundRect(barLeft, barY, barLeft + progressW, barY + dpToPx(2),
                    dpToPx(1), dpToPx(1), progressPaint);

            // 歌名
            String displayText = currentTitle;
            if (currentArtist != null && !currentArtist.isEmpty()) {
                displayText += " · " + currentArtist;
            }
            textPaint.setColor(0xFFE2E8F0);
            textPaint.setTextSize(dpToPx(9));
            float textX = dpToPx(22) + (isPlaying && !isMuted ? dpToPx(20) : 0);
            float textY = h / 2f + dpToPx(3);
            canvas.drawText(displayText, textX, textY, textPaint);
        }
    }

    // ===== 生命周期 =====
    @Override
    protected void onActivated() {
        super.onActivated();
        if (!isWaveAnimRunning) {
            startWaveAnimation();
        }
        setupMediaSession();
        updateMediaInfo();
    }

    @Override
    protected void onDeactivated() {
        super.onDeactivated();
        stopWaveAnimation();
        if (mediaController != null && mediaCallback != null) {
            mediaController.unregisterCallback(mediaCallback);
            mediaCallback = null;
        }
        mediaController = null;
        mainHandler.removeCallbacksAndMessages(null);
    }

    // ===== 交互 =====
    @Override
    protected void onIslandClick(@NonNull View v) {
        if (mediaController != null) {
            if (isPlaying) {
                mediaController.getTransportControls().pause();
            } else {
                mediaController.getTransportControls().play();
            }
        }
    }

    @Override
    protected void onIslandLongClick(@NonNull View v) {
        setExpanded(!isExpanded());
    }

    // ===== 必须实现的抽象方法 =====
    @Override
    public int getPriority() {
        return 50;
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
