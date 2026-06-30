package com.pulseisland.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PriorityEvaluator {

    public static final int PRIORITY_CRITICAL = 4; // 来电/闹钟/导航
    public static final int PRIORITY_HIGH = 3;     // 聊天消息
    public static final int PRIORITY_MEDIUM = 2;   // 游戏福利/App活动
    public static final int PRIORITY_LOW = 1;      // 广告/营销
    public static final int PRIORITY_NONE = 0;     // 不上岛

    // 重要消息包名白名单
    private static final Set<String> CRITICAL_PACKAGES = new HashSet<>(Arrays.asList(
            "com.android.dialer",           // 系统电话
            "com.google.android.dialer",
            "com.android.incallui"
    ));

    private static final Set<String> HIGH_PRIORITY_PACKAGES = new HashSet<>(Arrays.asList(
            "com.tencent.mm",               // 微信
            "com.tencent.mobileqq",         // QQ
            "com.android.mms",              // 短信
            "com.google.android.apps.messaging",
            "com.whatsapp",
            "org.telegram.messenger"
    ));

    private static final Set<String> MEDIUM_PRIORITY_PACKAGES = new HashSet<>(Arrays.asList(
            "com.autonavi.minimap",         // 高德地图
            "com.baidu.BaiduMap",           // 百度地图
            "com.tencent.map",              // 腾讯地图
            "com.sankuai.meituan",          // 美团
            "me.ele",                       // 饿了么
            "com.didi.global",              // 滴滴
            "com.didi.passenger"
    ));

    // 垃圾广告关键词
    private static final String[] SPAM_KEYWORDS = {
            "优惠券", "秒杀", "点击领取", "福利", "红包", "限时", "折扣",
            "邀请", "拼团", "砍价", "助力", "抽奖", "促销"
    };

    public static int evaluate(String packageName, String title, String content) {
        // 1. 关键级别（最高优先级）
        if (CRITICAL_PACKAGES.contains(packageName)) {
            return PRIORITY_CRITICAL;
        }

        // 2. 高优先级（聊天类）
        if (HIGH_PRIORITY_PACKAGES.contains(packageName)) {
            return PRIORITY_HIGH;
        }

        // 3. 中优先级（地图/外卖/打车）
        if (MEDIUM_PRIORITY_PACKAGES.contains(packageName)) {
            return PRIORITY_MEDIUM;
        }

        // 4. 检查是否包含垃圾广告关键词
        String fullText = (title + " " + content).toLowerCase();
        for (String keyword : SPAM_KEYWORDS) {
            if (fullText.contains(keyword)) {
                return PRIORITY_LOW;
            }
        }

        // 5. 默认：中优先级
        return PRIORITY_MEDIUM;
    }

    public static boolean shouldAutoExpand(int priority, String title, String content) {
        // 关键级别：自动展开
        if (priority == PRIORITY_CRITICAL) {
            return true;
        }

        // 高优先级：根据内容长度判断
        if (priority == PRIORITY_HIGH) {
            int totalLen = (title == null ? 0 : title.length()) +
                           (content == null ? 0 : content.length());
            return totalLen > 30; // 长消息自动展开
        }

        // 中优先级：默认不自动展开
        return false;
    }

    public static int calculateDisplayDuration(int priority, String title, String content) {
        int baseDuration;
        switch (priority) {
            case PRIORITY_CRITICAL:
                baseDuration = 6000; // 6秒
                break;
            case PRIORITY_HIGH:
                baseDuration = 4000; // 4秒
                break;
            case PRIORITY_MEDIUM:
                baseDuration = 2500; // 2.5秒
                break;
            default:
                baseDuration = 3000;
                break;
        }

        // 根据内容长度增加时间
        int totalLen = (title == null ? 0 : title.length()) +
                       (content == null ? 0 : content.length());
        if (totalLen > 50) {
            baseDuration += 1500;
        } else if (totalLen > 20) {
            baseDuration += 800;
        }

        return Math.min(baseDuration, 8000); // 最多8秒
    }
}
