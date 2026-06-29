package com.pulseisland.core;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;

public abstract class IslandLayout extends FrameLayout {

    // ===== 尺寸常量 =====
    protected static final float COMPACT_HEIGHT_DP = 22f;
    protected static final float COMPACT_WIDTH_DP = 126f;
    protected static final float EXPANDED_HEIGHT_DP = 44f;
    protected static final float EXPANDED_WIDTH_DP = 280f;
    protected static final float CORNER_RADIUS_COMPACT = 11f;
    protected static final float CORNER_RADIUS_EXPANDED = 18f;

    // ===== 动画参数 =====
    private static final float SPRING_STIFFNESS = 600f;
    private static final float SPRING_DAMPING = 0.75f;
    private static final int RADIUS_ANIM_DURATION_MS = 400;
    private static final int SWIPE_THRESHOLD_DP = 40;
    private static final int VERTICAL_SWIPE_TOLERANCE_DP = 60;

    // ===== 属性 =====
    protected float density;
    protected RectF rect = new RectF();
    protected float cornerRadius = CORNER_RADIUS_COMPACT;
    protected Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ===== 状态 =====
    private boolean isExpanded = false;
    private boolean isActive = false;

    // ===== 动画引擎 =====
    private SpringAnimation widthAnim;
    private SpringAnimation heightAnim;
    private ValueAnimator radiusAnim;

    // ===== 手势 =====
    private GestureDetector gestureDetector;
    private float totalScrollX = 0;
    private boolean isSwiping = false;
    private OnSwipeListener swipeListener;

    public interface OnSwipeListener {
        void onLeftSwipe();
        void onRightSwipe();
    }

    public IslandLayout(@NonNull Context context) {
        super(context);
        this.density = context.getResources().getDisplayMetrics().density;
        initLayout();
        initGestureDetector();
    }

    // ===== 初始化 =====
    private void initLayout() {
        setOnClickListener(v -> onIslandClick(this));
        setOnLongClickListener(v -> {
            onIslandLongClick(this);
            return true;
        });
        setWillNotDraw(false);
        initPaints();
        setLayoutParams(new FrameLayout.LayoutParams(
                dpToPx(COMPACT_WIDTH_DP),
                dpToPx(COMPACT_HEIGHT_DP)
        ));
    }

    private void initPaints() {
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(0xDD1E293B);
    }

    // ===== 单位转换 =====
    protected int dpToPx(float dp) {
        return (int) (dp * density + 0.5f);
    }

    // ===== 绘制 =====
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint);
    }

    // ===== 公开方法 =====
    public void setBackgroundColor(int color) {
        bgPaint.setColor(color);
        invalidate();
    }

    public void setSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }

    public void setActive(boolean active) {
        if (this.isActive == active) return;
        this.isActive = active;
        if (active) onActivated();
        else onDeactivated();
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    // ===== 子类回调 =====
    protected abstract void onIslandClick(@NonNull View v);
    protected abstract void onIslandLongClick(@NonNull View v);
    protected void onActivated() {}
    protected void onDeactivated() {}

    // ===== 展开/折叠（核心） =====
    public void setExpanded(boolean expanded) {
        if (this.isExpanded == expanded) return;
        this.isExpanded = expanded;

        int newWidth = dpToPx(expanded ? EXPANDED_WIDTH_DP : COMPACT_WIDTH_DP);
        int newHeight = dpToPx(expanded ? EXPANDED_HEIGHT_DP : COMPACT_HEIGHT_DP);
        float newRadius = expanded ? CORNER_RADIUS_EXPANDED : CORNER_RADIUS_COMPACT;

        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp == null) {
            setLayoutParams(new FrameLayout.LayoutParams(newWidth, newHeight));
            cornerRadius = newRadius;
            invalidate();
            return;
        }

        if (widthAnim == null) {
            widthAnim = new SpringAnimation(this, DynamicAnimation.WIDTH);
            widthAnim.getSpring().setStiffness(SPRING_STIFFNESS)
                     .setDampingRatio(SPRING_DAMPING);
        }
        widthAnim.animate// ===== 手势识别 =====
    private void initGestureDetector() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
                                    float distanceX, float distanceY) {
                float deltaX = e2.getX() - e1.getX();
                float deltaY = e2.getY() - e1.getY();
                if (Math.abs(deltaY) < dpToPx(VERTICAL_SWIPE_TOLERANCE_DP)) {
                    totalScrollX += deltaX;
                    isSwiping = true;
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                totalScrollX = 0;
                isSwiping = false;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isSwiping && Math.abs(totalScrollX) > dpToPx(SWIPE_THRESHOLD_DP)) {
                    if (totalScrollX < 0 && swipeListener != null) {
                        swipeListener.onLeftSwipe();
                    } else if (totalScrollX > 0 && swipeListener != null) {
                        swipeListener.onRightSwipe();
                    }
                } else if (!isSwiping) {
                    performClick();
                }
                isSwiping = false;
                totalScrollX = 0;
                return true;
        }
        return super.onTouchEvent(event);
    }
    }
