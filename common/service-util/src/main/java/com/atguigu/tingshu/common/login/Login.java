package com.atguigu.tingshu.common.login;

import java.lang.annotation.*;

/**
 * @author liutianba7
 * @create 2025/12/11 13:50
 *
 * 登录注解：被该注解标注的接口需要登录才能访问
 * com.atguigu.tingshu.common.login.LoginAspect 是具体切入细节
 */


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented

public @interface Login {
    boolean required() default true;
}
