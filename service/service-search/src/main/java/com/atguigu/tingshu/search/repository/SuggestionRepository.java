package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author liutianba7
 * @create 2025/12/16 09:30
 */
public interface SuggestionRepository extends ElasticsearchRepository<SuggestIndex, String> {

}
