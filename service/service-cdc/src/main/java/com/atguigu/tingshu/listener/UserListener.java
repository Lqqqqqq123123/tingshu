package com.atguigu.tingshu.listener;

import com.atguigu.tingshu.model.user.UserInfo;
import io.xzxj.canal.core.annotation.CanalListener;
import io.xzxj.canal.core.listener.EntryListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author liutianba7
 * @create 2025/12/19 18:08
 */

@Slf4j
@Component
@CanalListener(destination = "tingshuTopic", schemaName = "tingshu_user", tableName = "user_info")
public class UserListener implements EntryListener<UserInfo> {
    @Override
    public void insert(UserInfo userInfo) {
        EntryListener.super.insert(userInfo);
    }

    @Override
    public void update(UserInfo before, UserInfo after, Set<String> fields) {
        // 从缓存中删除
    }

    @Override
    public void delete(UserInfo userInfo) {
        EntryListener.super.delete(userInfo);
    }
}
