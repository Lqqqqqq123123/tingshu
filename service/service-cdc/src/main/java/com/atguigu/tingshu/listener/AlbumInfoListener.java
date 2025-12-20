package com.atguigu.tingshu.listener;

import com.atguigu.tingshu.model.album.AlbumInfo;
import io.xzxj.canal.core.annotation.CanalListener;
import io.xzxj.canal.core.listener.EntryListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author liutianba7
 * @create 2025/12/19 18:10
 */
@Slf4j
@Component
@CanalListener(destination = "tingshuTopic", schemaName = "tingshu_album", tableName = "album_info")
public class AlbumInfoListener implements EntryListener<AlbumInfo> {

    @Autowired
    private RedisTemplate redisTemplate;

    // @RedisCache(prefix = "album:albuminfo:", timeout = 3600) 缓存的配置
    @Override
    public void update(AlbumInfo before, AlbumInfo after, Set<String> fields) {
        log.info("更新前的:{}",  before);
        log.info("更新后的:{}",  after);
        // 从缓存中删除即可实现数据一致性
        // log.info("{}", before.getId());
        redisTemplate.delete("album:albuminfo:" + after.getId());
    }

    @Override
    public void insert(AlbumInfo albumInfo) {
        // log.info("新增数据:{}",  albumInfo);
    }

    @Override
    public void delete(AlbumInfo albumInfo) {
        log.info("删除数据:{}",  albumInfo);
        redisTemplate.delete("album:albuminfo:" + albumInfo.getId());
    }
}
