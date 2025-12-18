package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.cache.RedisCache;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    @RedisCache(prefix = "album:categoryList:", timeout = 3600 , timeunit = TimeUnit.SECONDS)
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

    /**
     * 根据一级分类id获取所有的标签以及标签对应的标签值
     * @param category1Id
     * @return List<BaseAttribute>
     */
    @Override
    @RedisCache(prefix = "album:attributeList:", timeout = 3600 , timeunit = TimeUnit.SECONDS)
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
    @RedisCache(prefix = "album:categoryView:", timeout = 3600 , timeunit = TimeUnit.SECONDS)
    public BaseCategoryView getCategoryView(Long category3Id) {

        LambdaQueryWrapper<BaseCategoryView> wr = new LambdaQueryWrapper<>();
        wr.eq(BaseCategoryView::getCategory3Id, category3Id);
        return baseCategoryViewMapper.selectOne(wr);
    }

    /**
     * // todo:之后把数据存放到Redis中，如果缓存中有，则从缓存中获取，如果没有，则从数据库中查询
     * 根据一级分类Id查询置顶7个三级分类列表
     * @param category1Id
     * @return
     */
    @Override
    @Transactional
    @RedisCache(prefix = "album:top7BaseCategory3:", timeout = 3600 , timeunit = TimeUnit.SECONDS)
    public List<BaseCategory3> findTopBaseCategory3(Long category1Id) {
        LambdaQueryWrapper<BaseCategoryView> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseCategoryView::getCategory1Id, category1Id);


        // 按照一级分类id查询三级分类id
        List<Long> list = baseCategoryViewMapper.selectList(wrapper)
                .stream()
                .map(BaseCategoryView::getCategory3Id)
                .toList();

        // 按照id批量查询三级分类
        // 过滤 is_top = 1，然后截取7个
        List<BaseCategory3> baseCategory3s = baseCategory3Mapper.selectBatchIds(list)
                .stream()
                .filter(item -> item.getIsTop() == 1)
                .limit(7)
                .sorted((t1, t2) ->{
                    return t1.getOrderNum() - t2.getOrderNum(); // 升序
                })
                .toList();

        return baseCategory3s;


    }

    /**
     * // todo:之后把数据存放到Redis中，如果缓存中有，则从缓存中获取，如果没有，则从数据库中查询
     * 查询1级分类下包含所有二级以及三级分类
     * @param category1Id
     * @return
     */

    @Override
    @RedisCache(prefix = "album:category2And3ListBy1Id:", timeout = 3600 , timeunit = TimeUnit.SECONDS)
    public JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {

        // 1. 查询1级分类下的所有二级分类
        List<BaseCategoryView> list = baseCategoryViewMapper.selectList(
                new LambdaQueryWrapper<BaseCategoryView>()
                        .eq(BaseCategoryView::getCategory1Id, category1Id)
        );

        // 2. 按照二级分类id分组，得到的就是该二级分类下的三级分类
        Map<Long, List<BaseCategoryView>> children = list.stream()
                .collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

        // 3. 先封装一个一个分类对象
        JSONObject result = new JSONObject();
        result.put("categoryId", category1Id);
        result.put("categoryName", list.get(0).getCategory1Name());
        List<JSONObject> category2List = new ArrayList<>();

        // 遍历集合，封装二级分类对象
        for(Map.Entry<Long, List<BaseCategoryView>> entry : children.entrySet()){
            JSONObject category2 = new JSONObject();
            category2.put("categoryId", entry.getKey());
            category2.put("categoryName", entry.getValue().get(0).getCategory2Name());
            List<Map<Long, String>> category3 = new ArrayList<>();
            for(BaseCategoryView item : entry.getValue()) {
                Map category3Item = new HashMap<>();
                category3Item.put("categoryId", item.getCategory3Id());
                category3Item.put("categoryName", item.getCategory3Name());
                category3.add(category3Item);
            }
            category2.put("categoryChild", category3);
            category2List.add(category2);
        }

        result.put("categoryChild", category2List);
        return result;
    }
}
