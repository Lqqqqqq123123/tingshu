package com.atguigu.tingshu.album.task;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author liutianba7
 * @create 2025/12/19 11:27
 */
@Component
@Slf4j
public class RebuildBloomFilterTask {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private AlbumInfoService albumInfoService;

    /**
     * 每月的凌晨两点，重建专辑 id 的布隆过滤器
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    // @Scheduled(cron = "0/59 * * * * ?")
    public void rebuildBloomFilter() {
        // 1. 拿到当前布隆过滤器的预估值
        RBloomFilter<Object> old_bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        long count = old_bloomFilter.count(); // 预估插入数量
        long exceptedInsertions = old_bloomFilter.getExpectedInsertions(); // 期望大小
        double falseProbability = old_bloomFilter.getFalseProbability();

        // all: 新建布隆过滤器
        RBloomFilter<Object> new_bloomFilter = redissonClient.getBloomFilter("temp");

        // 2. 如果预估值大于当前布隆过滤器的 exceptedInsertions，则扩容
        if(count > exceptedInsertions){
            // 2.1 扩容规则 exceptedInsertions * 1.5
            exceptedInsertions = (long) (exceptedInsertions * 1.5);

            // 2.2 初始化
            new_bloomFilter.tryInit(exceptedInsertions, falseProbability);

            // 2.3 删除旧的布隆过滤器
            boolean delete = old_bloomFilter.delete();
            if(delete){
                // 2.5 删除成功，则将新的布隆过滤器重命名为旧的布隆过滤器
                new_bloomFilter.rename(RedisConstant.ALBUM_BLOOM_FILTER);
            }

            // 2.4 添加专辑 id 到布隆过滤器中
            albumInfoService.batchUpperToBloom();
            log.info("扩容专辑 id 的布隆过滤器成功！");
        }else{
            // 3. 反之只需要重建即可（因为可能有很多专辑都下架了，但是布隆过滤器无法删除元素）
            // 3.1 初始化
            new_bloomFilter.tryInit(exceptedInsertions, falseProbability);

            // 3.2 删除旧的布隆过滤器
            boolean delete = old_bloomFilter.delete();
            if(delete){
                // 3.3 删除成功，则将新的布隆过滤器重命名为旧的布隆过滤器
                new_bloomFilter.rename(RedisConstant.ALBUM_BLOOM_FILTER);
            }

            // 3.4 添加专辑 id 到布隆过滤器中
            albumInfoService.batchUpperToBloom();

            log.info("重建专辑 id 的布隆过滤器成功！");
        }

    }
}
