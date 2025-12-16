package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.BusinessException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author liutianba7
 * @create 2025/12/11 13:52
 */
@Aspect
@Component
@Slf4j
public class LoginAspect {


    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 只切api包下的被登录注解的方法
     * @param joinPoint 切入点，也就是当前执行的方法
     * @return 方法的返回值
     * @throws Throwable 抛出的异常
     */

    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(login)")
    public Object authorize(ProceedingJoinPoint joinPoint, Login login) throws Throwable {

        // 1. 获取用户令牌，用于查询redis中用户基本信息
        log.info("LoginAspect前置逻辑");

        // 1.1 获取请求信息
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        // 1.2 获得请求对象
        ServletRequestAttributes requestAttributes1 = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = requestAttributes1.getRequest();

        // 1.3 获取用户令牌
        String token = request.getHeader("token");
        // 因为有些接口不登陆也能访问，所以这里不能因为令牌不存在就抛异常
//        if(!StringUtils.hasText(token)){
//            log.error("用户未登录");
//            throw new BusinessException(ResultCodeEnum.LOGIN_AUTH);
//        }

        // 2. 查询用户信息
        // 2.1 构建查询 key
        String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;

        // 2.2 查询
        UserInfoVo user = (UserInfoVo)redisTemplate.opsForValue().get(loginKey);

        // 2.3 如果用户信息不存在：没登陆或者令牌过期了
        if(login.required() && user == null){
                log.error("用户未登录");
                throw new BusinessException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 2.4 如果用户信息存在，则将用户信息保存到 ThreadLocal 中
        if(user != null)
            AuthContextHolder.setUserId(user.getId());


        // 3. 目标方法执行
        Object result = joinPoint.proceed();


        // 4. 记得清理 ThreadLocal中的数据
        log.info("后置逻辑");
        AuthContextHolder.removeUserId();


        return result;
    }

}
