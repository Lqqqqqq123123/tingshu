package com.atguigu.tingshu.listener;

import com.atguigu.tingshu.model.album.TrackInfo;
import io.xzxj.canal.core.annotation.CanalListener;
import io.xzxj.canal.core.listener.EntryListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author liutianba7
 * @create 2025/12/20 10:34
 * 实现专辑信息的数据一致性
 */
@Slf4j
@Component
@CanalListener(destination = "tingshuTopic", schemaName = "tingshu_album", tableName = "track_info")
public class TrackInfoListener implements EntryListener<TrackInfo> {
    @Autowired
    private RedisTemplate redisTemplate;

    // @RedisCache(prefix = "album:trackInfo:", timeout = 3600 , timeunit = TimeUnit.SECONDS)
    @Override
    public void update(TrackInfo before, TrackInfo after, Set<String> fields) {
        redisTemplate.delete("album:trackInfo:" + after.getId());
    }

    @Override
    public void delete(TrackInfo trackInfo) {
        redisTemplate.delete("album:trackInfo:" + trackInfo.getId());
    }
}
