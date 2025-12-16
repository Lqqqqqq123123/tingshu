package com.atguigu.tingshu.search.service;

import java.util.Map;

public interface ItemService {

    /**
     * 根据专辑ID汇总详情页所需参数
     *
     * @param albumId 专辑 id
     * @return 汇总数据： announcer albumInfo albumStatVo baseCategoryView
     */
    Map<String, Object> getItem(Long albumId);
}
