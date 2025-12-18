package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	@Autowired
	private BaseCategoryService baseCategoryService;


    @Operation(summary = "获取分类列表")
    @GetMapping("/category/getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList() {
        return Result.ok(baseCategoryService.getBaseCategoryList());
    }


    /**
     * 根据一级分类id获取所有的标签以及标签对应的标签值
     * @param category1Id
     * @return List<BaseAttribute>
     */
    @Operation(summary = "根据一级分类id获取所有的标签以及标签对应的标签值")
    @GetMapping("/category/findAttribute/{category1Id}")
    public Result<List<BaseAttribute>> getBaseAttributeListByCategory1Id(@PathVariable Long category1Id) {
        return Result.ok(baseCategoryService.getBaseAttributeListByCategory1Id(category1Id));
    }


    /**
     * 根据三级分类ID查询分类视图
     * @param category3Id
     * @return
     */
    @Operation(summary = "根据三级分类ID查询分类视图")
    @GetMapping("/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id){
        BaseCategoryView baseCategoryView = baseCategoryService.getCategoryView(category3Id);
        return Result.ok(baseCategoryView);
    }


    /**
     * 根据一级分类Id查询置顶7个三级分类列表
     * @param category1Id
     * @return
     */
    @Operation(summary = "根据一级分类Id查询置顶7个三级分类列表")
    @GetMapping("/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id) {
        List<BaseCategory3> list = baseCategoryService.findTopBaseCategory3(category1Id);
        return Result.ok(list);
    }

    /**
     * 查询1级分类下包含所有二级以及三级分类
     * @param category1Id
     * @return
     */
    @Operation(summary = "查询1级分类下包含所有二级以及三级分类")
    @GetMapping("/category/getBaseCategoryList/{category1Id}")
    public Result<JSONObject> getBaseCategoryListByCategory1Id(@PathVariable Long category1Id) {
        JSONObject jsonObject = baseCategoryService.getBaseCategoryListByCategory1Id(category1Id);
        return Result.ok(jsonObject);
    }

    /**
     * 查询所有的一级分类 id 与 名称
     * @return [{}, {} ... ]
     */

    @Operation(summary = "查询所有的一级分类 id 与 名称")
    @GetMapping("/category/findAllCategory1")
    public Result<List<BaseCategory1>> findAllCategory1(){
        List<BaseCategory1> list = baseCategoryService.list();
        return Result.ok(list);
    }
}

