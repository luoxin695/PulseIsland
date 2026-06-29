package com.pulseisland.utils;

import android.content.Context;

import moe.shizuku.manager.Shizuku;

public class ShizukuHelper {
    private static boolean isAvailable = false;
    private static OnPermissionChangedListener permissionListener;

    public interface OnPermissionChangedListener {
        void onPermissionChanged(boolean granted);
    }

    public static void init(Context context) {
        if (Shizuku.isAvailable() && !Shizuku.hasPermission()) {
            Shizuku.requestPermission(0);
        }
        isAvailable = Shizuku.isAvailable() && Shizuku.hasPermission();
        Shizuku.addBinderReceivedListener(() -> {
            if (!Shizuku.hasPermission()) {
                Shizuku.requestPermission(0);
            }
        });
        Shizuku.addBinderDeadListener(() -> {
            isAvailable = false;
            if (permissionListener != null) permissionListener.onPermissionChanged(false);
        });
        Shizuku.addPermissionListener(permissionCode -> {
            isAvailable = Shizuku.isAvailable() && Shizuku.hasPermission();
            if (permissionListener != null) permissionListener.onPermissionChanged(isAvailable);
        });
    }

    public static boolean isAvailable() { return isAvailable; }

    public static void setPermissionListener(OnPermissionChangedListener listener) {
        permissionListener = listener;
    }

    public static String execShell(String command) {
        if (!isAvailable) return "";
        try {
            return Shizuku.shell(command).execute().getOutput();
        } catch (Exception e) {
            return "";
        }
    }
}
