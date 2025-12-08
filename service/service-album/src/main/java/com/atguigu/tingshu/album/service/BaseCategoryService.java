package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    /**
     * todo : 使用redis去优化
     * 获取分类列表
     *
     * @return [{"categoryName":"军事","categoryId":1, [二级分类列表]}, {}, {}...]
     */
    List<JSONObject> getBaseCategoryList();


    /**
     * todo : 将数据放到缓存中优化
     * 根据一级分类id获取当前分类下的所有标签以及标签值
     * @param category1Id
     * @return
     */
    List<BaseAttribute> getBaseAttributeListByCategory1Id(Long category1Id);
}
