package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;

    @Override
    public List<JSONObject> getBaseCategoryList() {
        // 1. 去视图中查询全部三级分类
        List<BaseCategoryView> list = baseCategoryViewMapper.selectList(null);

        if(list == null || list.size() == 0){
            return new ArrayList<>();
        }
        // 2. 遍历结果，然后分装为前端要求的数据形式
        // 2.1 先按照一级分类分组
        Map<Long, List<BaseCategoryView>> g1 = list
                .stream()
                .collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        // 2.2 构造一级分类列表
        List<JSONObject> result = new ArrayList<>();

        for(Map.Entry<Long, List<BaseCategoryView>> entry : g1.entrySet()){
            Long category1Id = entry.getKey();
            String category1Name = entry.getValue().get(0).getCategory1Name(); // 所有的一级分类名称都相同，所以取第一个

            // 2.3 再去更具二级分类分组
            Map<Long, List<BaseCategoryView>> g2 = entry.getValue()
                    .stream()
                    .collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

            // 2.4 构造二级分类列表
            List<JSON> category2List = new ArrayList<>();
            for(Map.Entry<Long, List<BaseCategoryView>> entry2 : g2.entrySet()){
                Long category2Id = entry2.getKey();
                String category2Name = entry2.getValue().get(0).getCategory2Name(); // 所有的二级分类名称都相同，所以取第一个

                List<JSONObject> category3List = entry2.getValue()
                        .stream()
                        .map(item -> {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("categoryId", item.getCategory3Id());
                            jsonObject.put("categoryName", item.getCategory3Name());
                            return jsonObject;
                        })
                        .collect(Collectors.toList());

                JSONObject category2 = new JSONObject();
                category2.put("categoryId", category2Id);
                category2.put("categoryName", category2Name);
                category2.put("categoryChild", category3List);

                category2List.add(category2);
            }

            JSONObject category1 = new JSONObject();
            category1.put("categoryId", category1Id);
            category1.put("categoryName", category1Name);
            category1.put("categoryChild", category2List);
            result.add(category1);
        }

        // 3. 返回结果
        return result;
    }

    @Override
    public List<BaseAttribute> getBaseAttributeListByCategory1Id(Long category1Id) {
        // 1. 将表 base_attribute 与 base_attribute_value 关联
        return baseAttributeMapper.getBaseAttributeListByCategory1Id(category1Id);
    }

    /**
     * 根据三级分类ID查询分类视图
     * @param category3Id
     * @return
     */
    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {

        LambdaQueryWrapper<BaseCategoryView> wr = new LambdaQueryWrapper<>();
        wr.eq(BaseCategoryView::getCategory3Id, category3Id);
        return baseCategoryViewMapper.selectOne(wr);
    }
}
