package com.atguigu.tingshu.common.cache;

import cn.hutool.core.collection.CollectionUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author liutianba7
 * @create 2025/12/18 17:13
 */
@Aspect
@Slf4j
@Component
public class RedisCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    // 只切被 @RedisCache 注解的方法
    @Around("@annotation(cache)")
    public Object around(ProceedingJoinPoint joinPoint, RedisCache cache) throws Throwable {
        try {
            // 前置逻辑
            log.info("liutianba7：RedisCacheAspect【前置逻辑】");
            // 1. 优先从 Redis 中获取数据，命中缓存，返回数据
            // 1.1 构建业务数据的缓存key 自定义前缀 + 目标方法参数（多个参数用下划线凭借）
            List<Object> args = Arrays.asList(joinPoint.getArgs());
            String args_string = "null";
            if(CollectionUtil.isNotEmpty(args))
            {
                args_string = args.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining("_"));
            }
            String dataKey = cache.prefix() + args_string;

            // 1.2 获取缓存数据 这里要实现动态的转为目标方法的返回值
            Object data = redisTemplate.opsForValue().get(dataKey);
            if(data instanceof  CacheDataWrapper){
                log.info("liutianba7：RedisCacheAspect：" + cache.prefix() + "【缓存命中】");
                return ((CacheDataWrapper) data).getData();
            }


//            if(data != null){
//                log.info("liutianba7：RedisCacheAspect：" + cache.prefix() + "【缓存命中】");
//                return data;
//            }

            // 2. 缓存中没有，则获取分布式锁
            // 2.1 构建锁的缓存key 业务数据key:lock
            String lockKey = dataKey + RedisConstant.CACHE_LOCK_SUFFIX;
            // 2.2 获取锁实例
            RLock lock = redissonClient.getLock(lockKey);
            // 2.3 尝试获取分布式锁
            boolean is_locked = lock.tryLock(RedisConstant.ALBUM_LOCK_WAIT_PX1, RedisConstant.ALBUM_LOCK_EXPIRE_PX2, TimeUnit.SECONDS);


            // 3. 获取锁成功，则执行目标方法，从数据库中获取数据
            if(is_locked){
                try{
                    log.info("liutianba7：RedisCacheAspect【目标方法执行】");
                    data = joinPoint.proceed();
                    // 3.1 将数据写入缓存中
                    // error:data可能是 list 类型，如果直接写入，反序列化会出错
                    // redisTemplate.opsForValue().set(dataKey, data, cache.timeout() + new Random().nextInt(1000), cache.timeunit());
                    // 写入时
                    CacheDataWrapper wrapper = new CacheDataWrapper(data);
                    redisTemplate.opsForValue().set(dataKey, wrapper, cache.timeout(), cache.timeunit());
                    // 3.2 返回数据
                    return data;
                }finally {
                    // 5. 释放锁
                    lock.unlock();
                }
            }else{

                // 4. 获取所失败，自旋
                log.info("liutianba7：RedisCacheAspect【获取锁失败，自旋】");
                TimeUnit.MICROSECONDS.sleep(30);
                return around(joinPoint, cache);

            }
        } catch (Throwable e) {
            log.error("liutianba7：RedisCacheAspect【redis 服务出现问题,直接从数据库获取】");
            log.error(e.toString());
            return joinPoint.proceed();
        }
    }
}
