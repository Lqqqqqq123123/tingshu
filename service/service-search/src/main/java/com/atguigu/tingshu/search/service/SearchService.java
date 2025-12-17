package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.search.AlbumUpdateStatVo;

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

    /**
     * 保存suggest索引
     * @param po
     */
    void saveSuggestIndex(AlbumInfoIndex po);

    /**
     * 根据用户已录入字符查询提词索引库进行自动补全关键字
     * @param keyword
     * @return
     */
    List<String> completeSuggest(String keyword);

    /**
     * // todo:这里传入的参数是 TrackStatMqVo, 后续该方法可能重载,因为专辑的其他统计信息也会被更改,所以最好重载为 statType, Count, AlbumId,可以用一个Vo来封装
     * 更新 es 中专辑的统计信息
     * @param vo 专辑更新统计信息 vo{albumid, statType, count}
     */
    void updateAlbumStat(AlbumUpdateStatVo vo);
}
