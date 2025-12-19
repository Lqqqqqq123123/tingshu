package com.atguigu.tingshu.listener;

import com.atguigu.tingshu.model.album.AlbumInfo;
import io.xzxj.canal.core.annotation.CanalListener;
import io.xzxj.canal.core.listener.EntryListener;
import lombok.extern.slf4j.Slf4j;
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
    public AlbumInfoListener() {
        System.out.println(">>>>>>>>> Canal监听器类被Spring加载了 <<<<<<<<<");
    }
    // ... 其他代码
    @Override
    public void update(AlbumInfo before, AlbumInfo after, Set<String> fields) {
        log.info("更新前的:{}",  before);
        log.info("更新后的:{}",  after);
        // 从缓存中删除即可实现数据一致性

    }

    @Override
    public void insert(AlbumInfo albumInfo) {
        log.info("新增数据:{}",  albumInfo);
    }

    @Override
    public void delete(AlbumInfo albumInfo) {
        log.info("删除数据:{}",  albumInfo);
    }
}
