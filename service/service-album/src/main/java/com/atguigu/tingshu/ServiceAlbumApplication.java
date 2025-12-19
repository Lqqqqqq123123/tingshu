package com.atguigu.tingshu;

import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@Slf4j
public class ServiceAlbumApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);
    }


    @Autowired
    private RedissonClient redissonClient;

    /**
     * SpringBoot 程序启动后执行
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        // 初始化布隆过滤器
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);

        // 如过不存在，才去初始化
        if(!bloomFilter.isExists()){
            log.info("liutianba7：初始化专辑布隆过滤器");
            bloomFilter.tryInit(10000L, 0.05);
        }
    }
}
