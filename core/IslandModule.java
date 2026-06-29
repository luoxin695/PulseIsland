package com.pulseisland.core;

import android.content.Context;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 脉搏岛模块接口 —— 每个"岛功能"都必须实现此接口
 * 设计目标：可插拔、防泄漏、防重复、与 IslandLayout 解耦协作
 */
public interface IslandModule {

    /**
     * 获取模块视图（单例缓存）
     * 多次调用应返回同一个 View 实例
     * 注意：传入的 Context 必须是 ApplicationContext，严禁传入 Activity
     */
    @NonNull
    View getView(@NonNull Context context);

    /**
     * 模块被激活时调用（例如切换到前台、获得焦点）
     * 可在此注册监听器、启动动画
     */
    void onActivated();

    /**
     * 模块被停用时调用（例如被顶替、失去焦点）
     * 必须在此清理资源（注销监听器、停止动画、释放引用）
     */
    void onDeactivated();

    /**
     * 模块被点击时调用（由 IslandLayout 统一转发）
     * @param v 被点击的视图（即 IslandLayout 实例）
     */
    void onClick(@NonNull View v);

    /**
     * 当前模块是否处于激活状态
     */
    boolean isActive();

    /**
     * 扩展级别：0=不扩展，1=向右扩展，2=向左扩展，3=双向扩展
     * 配合 IslandLayout 的 setExpanded 使用，决定展开方向
     */
    @ExpandLevel
    int getExpandLevel();

    /**
     * 模块创建时间（毫秒时间戳）
     */
    long getCreateTime();

    /**
     * 模块是否可以被用户划掉关闭
     */
    boolean isDismissible();

    /**
     * 优先级：数字越大优先级越高
     * 高优先级模块可以顶替低优先级模块
     */
    int getPriority();

    /**
     * 是否为常驻模块（true 表示不会被自动清理）
     */
    boolean isPersistent();

    /**
     * 自动消失时间（毫秒），0 表示不自动消失
     */
    long getAutoDismissMs();

    // ===== 注解定义（编译期检查） =====
    @IntDef({ExpandLevel.NONE, ExpandLevel.RIGHT, ExpandLevel.LEFT, ExpandLevel.BOTH})
    @Retention(RetentionPolicy.SOURCE)
    @interface ExpandLevel {
        int NONE = 0;
        int RIGHT = 1;
        int LEFT = 2;
        int BOTH = 3;
    }
}
