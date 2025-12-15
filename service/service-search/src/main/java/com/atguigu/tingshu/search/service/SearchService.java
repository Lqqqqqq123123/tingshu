package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SearchService {

    /**
     * 将指定专辑上架到索引库
     * @param albumId 专辑ID
     * @return
     */
    void upperAlbum(Long albumId);

    /**
     * 将指定专辑下架，从索引库删除文档
     * @param albumId
     * @return
     */
    void lowerAlbum(Long albumId);


    /**
     * 批量上架专辑(不严谨版本，也就是直接遍历  1-maxid)
     */
    public void batchUpperAlbum();

    /**
     * 站内搜索
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    /**
     * 构建DSL
     * @param albumIndexQuery
     * @return
     */
    SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery);

    /**
     * 解析响应结果
     * @param resp
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo parseResponse(SearchResponse<AlbumInfoIndex> resp, AlbumIndexQuery albumIndexQuery);

    /**
     * 查询1级分类下置顶3级分类热度TOP6专辑
     * @param category1Id
     * @return [{"baseCategory3":{三级分类对象},list:[专辑列表]},,{其他6个置顶分类热门专辑Map}]
     */
    List<Map<String, Object>> channel(Long category1Id) throws IOException;
}
