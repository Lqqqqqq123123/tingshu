package com.atguigu.tingshu.listener;

import com.atguigu.tingshu.model.user.UserInfo;
import io.xzxj.canal.core.annotation.CanalListener;
import io.xzxj.canal.core.listener.EntryListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author liutianba7
 * @create 2025/12/19 18:08
 */

@Slf4j
@Component
@CanalListener(destination = "tingshuTopic", schemaName = "tingshu_user", tableName = "user_info")
public class UserInfoListener implements EntryListener<UserInfo> {

    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public void insert(UserInfo userInfo) {
        //  新增不需要做缓存
        // log.info("新增用户：{}");
    }

    // @RedisCache(prefix = "user:userInfoVo:", timeout = 3600) 这是用户数据的缓存
    @Override
    public void update(UserInfo before, UserInfo after, Set<String> fields) {
        // 从缓存中删除原来的用户
        redisTemplate.delete("user:userInfoVo:" + after.getId());
        log.info("删除缓存中用户{}", after.getId());

    }

    @Override
    public void delete(UserInfo userInfo) {
        // 从缓存中删除该用户
        redisTemplate.delete("user:userInfoVo:" + userInfo.getId());
    }
}
