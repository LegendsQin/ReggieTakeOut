package org.example.reggie.common;

/**
 * 基于ThreadLocal封装的工具类，用于存储和获取当前登录用户的ID
 */
public class BaseContext {
    // ThreadLocal 泛型根据你的用户ID类型定（通常是Long/Integer）
    // 一个 ThreadLocal 实例 被所有线程共享
    // 每个线程在同一个 ThreadLocal 实例中存储 独立的值副本
    // 每个线程调用 set 方法设置值时，实际上是将值存储在该线程的本地存储区域中
    // 每个线程调用 get 方法获取值时，实际上是从该线程的本地存储区域中获取值
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前登录用户的ID
     * @param userId
     */
    public static void setCurrentUserId(Long userId) {
        threadLocal.set(userId);
    }

    /**
     * 获取当前登录用户的ID
     * @return
     */
    public static Long getCurrentUserId() {
        return threadLocal.get();
    }

    /**
     * 移除当前登录用户的ID
     */
    public static void removeCurrentUserId() {
        threadLocal.remove();
    }
}
