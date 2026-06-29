package com.pulseisland.utils;

import android.content.Context;

public class DeviceAdapter {
    public static int getIslandY(Context context) {
        int statusBarHeight = getStatusBarHeight(context);
        return statusBarHeight + dpToPx(context, 30);
    }

    public static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return dpToPx(context, 24);
    }

    public static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
