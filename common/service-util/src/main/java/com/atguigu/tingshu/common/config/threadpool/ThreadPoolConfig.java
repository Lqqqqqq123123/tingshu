package com.atguigu.tingshu.common.config.threadpool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * @author liutianba7
 * @create 2025/12/13 12:15
 */
@Configuration
public class ThreadPoolConfig {

    // java juc 提供的线程池
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int threadCount = corePoolSize * 2;

        return new ThreadPoolExecutor(
                corePoolSize,
                threadCount,
                0,
                TimeUnit.SECONDS ,
                new ArrayBlockingQueue<>(200),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 基于Spring提供线程池Class-threadPoolTaskExecutor 功能更强
     */
    @Bean
    // @Primary
    public Executor threadPoolTaskExecutor() {
        int count = Runtime.getRuntime().availableProcessors();
        int threadCount = count*2+1;
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // 核心池大小
        taskExecutor.setCorePoolSize(threadCount);
        // 最大线程数
        taskExecutor.setMaxPoolSize(threadCount);
        // 队列程度
        taskExecutor.setQueueCapacity(300);
        // 线程空闲时间
        taskExecutor.setKeepAliveSeconds(0);
        // 线程前缀名称
        taskExecutor.setThreadNamePrefix("sync-tingshu-Executor--");
        // 该方法用来设置 线程池关闭 的时候 等待 所有任务都完成后，再继续 销毁 其他的 Bean，
        // 这样这些 异步任务 的 销毁 就会先于 数据库连接池对象 的销毁。
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        // 任务的等待时间 如果超过这个时间还没有销毁就 强制销毁，以确保应用最后能够被关闭，而不是阻塞住。
        taskExecutor.setAwaitTerminationSeconds(300);
        // 线程不够用时由调用的线程处理该任务
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return taskExecutor;
    }
}
