package com.atguigu.tingshu.listener;

import com.atguigu.tingshu.model.user.VipServiceConfig;
import io.xzxj.canal.core.annotation.CanalListener;
import io.xzxj.canal.core.listener.EntryListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author liutianba7
 * @create 2025/12/20 11:11
 */
@Component
@CanalListener(destination = "tingshuTopic", schemaName = "tingshu_user", tableName = "vip_service_config")
public class VipConfigListener implements EntryListener<VipServiceConfig> {

    @Autowired
    private RedisTemplate redisTemplate;

    // @RedisCache(prefix = "user:vipconfig:", timeout = 3600) 单个配置的缓存
    // @RedisCache(prefix = "user:vipconfig:list", timeout = 3600) 列表的缓存
    @Override
    public void update(VipServiceConfig before, VipServiceConfig after, Set<String> fields) {
        redisTemplate.delete("user:vipconfig:" + after.getId());
        redisTemplate.delete("user:vipconfig:list:" + after.getId());
    }
}
