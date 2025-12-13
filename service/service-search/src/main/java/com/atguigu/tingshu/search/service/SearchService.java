package com.atguigu.tingshu.search.service;

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
}
