package com.atguigu.tingshu.common.cache;


import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 该注解用来表示哪些方法的数据需要从缓存中获取，缓存中没有才从数据库中获取
 * @author liutianba7
 * @create 2025/12/18 17:07
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCache {
    String prefix() default  "";                    // 业务数据以及分布式锁的缓存前缀
    int timeout() ;                                 // 业务数据的缓存时间
    TimeUnit timeunit() default TimeUnit.SECONDS;   // 业务数据的缓存时间单位
}
