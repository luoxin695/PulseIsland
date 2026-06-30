package com.pulseisland.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.pulseisland.modules.MessageModule;
import com.pulseisland.utils.PriorityEvaluator;
import com.pulseisland.utils.ShizukuHelper;

public class IslandNotificationListener extends NotificationListenerService {

    private static final String TAG = "IslandNotifListener";
    private static IslandNotificationListener instance;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isProcessing = false;
    private boolean isDragMode = false;
    private boolean isDropDownMode = false;

    // 消息模块引用（由 IslandService 注入）
    private MessageModule messageModule;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannel();
        // 初始化 Shizuku
        ShizukuHelper.init(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (isProcessing) return;
        if (isDragMode || isDropDownMode) {
            // 拖拽或下拉模式下，延迟处理消息
            mainHandler.postDelayed(() -> processNotification(sbn), 500);
            return;
        }
        processNotification(sbn);
    }

    private void processNotification(StatusBarNotification sbn) {
        isProcessing = true;
        try {
            String packageName = sbn.getPackageName();
            Notification notification = sbn.getNotification();
            String title = "";
            String content = "";
            int priority = 0;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                title = notification.extras.getString(Notification.EXTRA_TITLE, "");
                content = notification.extras.getString(Notification.EXTRA_TEXT, "");
            }

            // 判断是否重要
            int importance = PriorityEvaluator.evaluate(packageName, title, content);
            if (importance == PriorityEvaluator.PRIORITY_LOW) {
                isProcessing = false;
                return; // 不上岛
            }

            // 判断是否需要自动展开
            boolean shouldExpand = PriorityEvaluator.shouldAutoExpand(importance, title, content);

            // 通知 MessageModule 显示消息
            if (messageModule != null) {
                mainHandler.post(() -> {
                    messageModule.showMessage(packageName, title, content, importance, shouldExpand);
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "处理通知失败: " + e.getMessage());
        } finally {
            isProcessing = false;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // 消息被清除时，通知 MessageModule
        if (messageModule != null) {
            mainHandler.post(() -> {
                messageModule.onMessageDismissed(sbn.getPackageName());
            });
        }
    }

    public void setMessageModule(MessageModule module) {
        this.messageModule = module;
    }

    public void setDragMode(boolean enabled) {
        this.isDragMode = enabled;
    }

    public void setDropDownMode(boolean enabled) {
        this.isDropDownMode = enabled;
    }

    public static IslandNotificationListener getInstance() {
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "island_notif_channel",
                    "灵动岛通知",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("脉搏岛用于监听通知的服务通道");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
      }
