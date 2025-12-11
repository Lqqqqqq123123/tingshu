package com.atguigu.tingshu.common.util;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 获取当前用户信息帮助类
 * ThreadLocal：绑定线程的变量，线程隔离，线程安全
 * 缺点：无法在父子线程中传递参数
 * 解决方法：使用阿里提供的 TransmittableThreadLocal
 */
public class AuthContextHolder {

    private static ThreadLocal<Long> userId = new TransmittableThreadLocal<>();
    // private static TransmittableThreadLocal<Long> userId = new TransmittableThreadLocal<Long>();

    public static void setUserId(Long _userId) {
        userId.set(_userId);
    }

    // 暂时先硬编码，因为还没写登录 td已完成
    public static Long getUserId() {
        // return 1L;
        return userId.get();
    }

    public static void removeUserId() {
        userId.remove();
    }

}
