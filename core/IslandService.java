package com.pulseisland.core;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pulseisland.utils.DeviceAdapter;

/**
 * 灵动岛服务 —— 负责在系统顶层显示岛
 * 职责：窗口管理 + 模块调度 + 传感器交互
 * 注意：需要 SYSTEM_ALERT_WINDOW 权限
 */
public class IslandService extends Service implements SensorEventListener {

    // ===== 常量 =====
    private static final int SENSOR_DELAY_MICROS = 100000; // 100ms

    // ===== 系统服务 =====
    private WindowManager windowManager;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    // ===== UI 组件 =====
    private FrameLayout islandContainer;
    private WindowManager.LayoutParams windowParams;

    // ===== 当前模块 =====
    private IslandModule currentModule;
    private IslandLayout currentLayout;

    // ===== 临时状态 =====
    private View transientView;
    private boolean isTransientShowing = false;
    private Runnable transientDismissRunnable;

    // ===== 传感器 =====
    private boolean isSensorRegistered = false;

    // ===== 主线程 Handler =====
    private final Handler mainHandler = new Handler();

    // ===== 生命周期 =====
    @Override
    public void onCreate() {
        super.onCreate();
        initWindowManager();
        initSensors();
        createContainer();
        addContainerToWindow();
    }

    private void initWindowManager() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SENSOR_DELAY_MICROS);
                isSensorRegistered = true;
            }
        }
    }

    private void createContainer() {
        islandContainer = new FrameLayout(this);
        islandContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        islandContainer.setClickable(false);
    }

    private void addContainerToWindow() {
        windowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        windowParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        windowParams.y = DeviceAdapter.getIslandY(this);
        windowManager.addView(islandContainer, windowParams);
    }

    // ===== 公开 API =====

    /**
     * 设置当前显示的模块
     * @param module 新模块，null 表示清空
     */
    public void setModule(@Nullable IslandModule module) {
        if (module == null) {
            clearModule();
            return;
        }
        if (module == currentModule) return;

        if (currentModule != null) {
            currentModule.onDeactivated();
        }

        View view = module.getView(this);
        if (!(view instanceof IslandLayout)) {
            throw new IllegalArgumentException("Module view must be an IslandLayout");
        }

        IslandLayout newLayout = (IslandLayout) view;
        islandContainer.removeAllViews();
        islandContainer.addView(newLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        currentModule = module;
        currentLayout = newLayout;
        currentModule.onActivated();

        newLayout.setSwipeListener(new IslandLayout.OnSwipeListener() {
            @Override
            public void onLeftSwipe() {
                // 外部可通过接口回调处理
            }

            @Override
            public void onRightSwipe() {
                // 外部可通过接口回调处理
            }
        });
              }// ===== 展开/折叠控制 =====
    public void expand() {
        if (currentLayout != null) {
            currentLayout.setExpanded(true);
        }
    }

    public void collapse() {
        if (currentLayout != null) {
            currentLayout.setExpanded(false);
        }
    }

    public void toggleExpand() {
        if (currentLayout != null) {
            currentLayout.setExpanded(!currentLayout.isExpanded());
        }
    }

    // ===== 清空模块 =====
    public void clearModule() {
        if (currentModule != null) {
            currentModule.onDeactivated();
            currentModule = null;
        }
        islandContainer.removeAllViews();
        currentLayout = null;
        if (transientDismissRunnable != null) {
            mainHandler.removeCallbacks(transientDismissRunnable);
        }
        isTransientShowing = false;
    }

    // ===== 显示临时视图 =====
    public void showTransientView(View view, int durationMs) {
        if (isTransientShowing) return;
        isTransientShowing = true;

        IslandModule savedModule = currentModule;
        IslandLayout savedLayout = currentLayout;

        if (currentModule != null) {
            currentModule.onDeactivated();
        }

        islandContainer.removeAllViews();
        islandContainer.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        transientView = view;

        transientDismissRunnable = () -> {
            if (!isTransientShowing) return;
            isTransientShowing = false;
            islandContainer.removeView(view);
            transientView = null;

            if (savedModule != null && savedLayout != null) {
                islandContainer.removeAllViews();
                islandContainer.addView(savedLayout);
                currentModule = savedModule;
                currentLayout = savedLayout;
                currentModule.onActivated();
            } else {
                clearModule();
            }
        };
        mainHandler.postDelayed(transientDismissRunnable, durationMs);
    }

    // ===== 传感器回调 =====
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float acceleration = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        if (acceleration > 3.0f) {
            toggleExpand();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 可忽略
    }

    // ===== Service 生命周期 =====
    @Override
    public void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);

        if (isSensorRegistered && sensorManager != null) {
            sensorManager.unregisterListener(this);
           // ===== 展开/折叠控制 =====
    public void expand() {
        if (currentLayout != null) {
            currentLayout.setExpanded(true);
        }
    }

    public void collapse() {
        if (currentLayout != null) {
            currentLayout.setExpanded(false);
        }
    }

    public void toggleExpand() {
        if (currentLayout != null) {
            currentLayout.setExpanded(!currentLayout.isExpanded());
        }
    }

    // ===== 清空模块 =====
    public void clearModule() {
        if (currentModule != null) {
            currentModule.onDeactivated();
            currentModule = null;
        }
        islandContainer.removeAllViews();
        currentLayout = null;
        if (transientDismissRunnable != null) {
            mainHandler.removeCallbacks(transientDismissRunnable);
        }
        isTransientShowing = false;
    }

    // ===== 显示临时视图 =====
    public void showTransientView(View view, int durationMs) {
        if (isTransientShowing) return;
        isTransientShowing = true;

        IslandModule savedModule = currentModule;
        IslandLayout savedLayout = currentLayout;

        if (currentModule != null) {
            currentModule.onDeactivated();
        }

        islandContainer.removeAllViews();
        islandContainer.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        transientView = view;

        transientDismissRunnable = () -> {
            if (!isTransientShowing) return;
            isTransientShowing = false;
            islandContainer.removeView(view);
            transientView = null;

            if (savedModule != null && savedLayout != null) {
                islandContainer.removeAllViews();
                islandContainer.addView(savedLayout);
                currentModule = savedModule;
                currentLayout = savedLayout;
                currentModule.onActivated();
            } else {
                clearModule();
            }
        };
        mainHandler.postDelayed(transientDismissRunnable, durationMs);
    }

    // ===== 传感器回调 =====
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float acceleration = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        if (acceleration > 3.0f) {
            toggleExpand();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 可忽略
    }

    // ===== Service 生命周期 =====
    @Override
    public void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);

        if (isSensorRegistered && sensorManager != null) {
            sensorManager.unregisterListener(this);
            isSensorRegistered = false;
        }

        if (windowManager != null && islandContainer != null) {
            try {
                windowManager.removeView(islandContainer);
            } catch (IllegalArgumentException ignored) {
                // 可能已被移除
            }
        }

        if (currentModule != null) {
            currentModule.onDeactivated();
            currentModule = null;
        }
        currentLayout = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ===== 权限检查 =====
    public static boolean hasOverlayPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(context);
    }
} isSensorRegistered = false;
        }

        if (windowManager != null && islandContainer != null) {
            try {
                windowManager.removeView(islandContainer);
            } catch (IllegalArgumentException ignored) {
                // 可能已被移除
            }
        }

        if (currentModule != null) {
            currentModule.onDeactivated();
            currentModule = null;
        }
        currentLayout = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ===== 权限检查 =====
    public static boolean hasOverlayPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(context);
    }
}
