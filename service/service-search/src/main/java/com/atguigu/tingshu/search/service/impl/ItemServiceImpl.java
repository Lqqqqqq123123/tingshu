package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.BusinessException;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {


    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private Executor threadPoolTaskExecutor;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 根据专辑ID汇总详情页所需参数
     *
     * @param albumId 专辑 id
     * @return 汇总数据： announcer albumInfo albumStatVo baseCategoryView
     */
    @Override
    public Map<String, Object> getItem(Long albumId) {
        // 1. 创建最终返回结果
        // 四个子任务都在写 hashmap, 存在线程不安全的问题，所以用线程安全的集合
        Map<String, Object> item = new ConcurrentHashMap<>();
        // 异步编排优化
        // 分析： 查询专辑信息和专辑统计信息是可以异步进行的，而专辑分类以及专辑作者信息则依赖于专辑信息


        // 拿到布隆过滤器
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);

        // 2. 获取专辑信息
        CompletableFuture<AlbumInfo> task1 = CompletableFuture.supplyAsync(() -> {
            // 2.1 todo:通过布隆过滤器来判断专辑是否存在，用来解决缓存穿透问题
//            boolean contains = bloomFilter.contains(albumId);
//            if(!contains){
//                log.info("liutianba7：专辑{}不存在于数据库", albumId);
//                throw new BusinessException(404, "专辑不存在");
//            }
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "getItem : 专辑信息不存在"); // 因为布隆过滤器可能存在误判，所以这里要判断一下
            item.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolTaskExecutor);


        // 3. 获取专辑统计信息
        CompletableFuture<Void> task2 = CompletableFuture.runAsync(() -> {
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "getItem : 专辑统计信息不存在");
            item.put("albumStatVo", albumStatVo);
        }, threadPoolTaskExecutor);

        // 4. 获取专辑分类信息
        CompletableFuture<Void> task3 = task1.thenAcceptAsync(albumInfo -> {
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "getItem : 专辑分类信息不存在");
            item.put("baseCategoryView", baseCategoryView);
        }, threadPoolTaskExecutor);


        // 5. 获取专辑作者信息
        CompletableFuture<Void> task4 = task1.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "getItem : 专辑作者信息不存在");
            item.put("announcer", userInfoVo);
        }, threadPoolTaskExecutor);


        // 在 join 之前
        try {
            // 6. 编排：要求所有任务都完成，才能返回结果
            CompletableFuture.allOf(task1, task2, task3, task4).orTimeout(2, TimeUnit.SECONDS).join();
        } catch (CompletionException e) {
            // 显式抛出其内部的原始异常
            if (e.getCause() instanceof BusinessException) {
                throw (BusinessException) e.getCause();
            }
            throw e;
        }

        return item;

    }
}
