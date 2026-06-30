package com.pulseisland.utils;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

public class NotificationParser {

    public static ParsedNotification parse(StatusBarNotification sbn) {
        ParsedNotification result = new ParsedNotification();
        result.packageName = sbn.getPackageName();
        result.postTime = sbn.getPostTime();

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        result.title = extras.getString(Notification.EXTRA_TITLE, "");
        result.content = extras.getString(Notification.EXTRA_TEXT, "");
        result.subText = extras.getString(Notification.EXTRA_SUB_TEXT, "");
        result.infoText = extras.getString(Notification.EXTRA_INFO_TEXT, "");
        result.summaryText = extras.getString(Notification.EXTRA_SUMMARY_TEXT, "");

        // 判断是否包含图片（部分通知有图片）
        result.hasImage = extras.get(Notification.EXTRA_PICTURE) != null;

        // 提取通知分类
        result.category = notification.category;

        // 提取 PendingIntent（用于跳转）
        result.contentIntent = notification.contentIntent;

        return result;
    }

    public static class ParsedNotification {
        public String packageName;
        public String title;
        public String content;
        public String subText;
        public String infoText;
        public String summaryText;
        public String category;
        public long postTime;
        public boolean hasImage;
        public PendingIntent contentIntent;

        public boolean hasTitle() {
            return title != null && !title.isEmpty();
        }

        public boolean hasContent() {
            return content != null && !content.isEmpty();
        }

        public String getDisplayText() {
            if (hasTitle() && hasContent()) {
                return title + "：" + content;
            } else if (hasTitle()) {
                return title;
            } else if (hasContent()) {
                return content;
            }
            return "新通知";
        }

        public String getShortDisplay() {
            String full = getDisplayText();
            if (full.length() > 30) {
                return full.substring(0, 28) + "…";
            }
            return full;
        }
    }
}
