package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author liutianba7
 * @create 2025/12/13 09:19
 */
public interface AlbumInfoRepository extends ElasticsearchRepository<AlbumInfoIndex, Long> {
}
