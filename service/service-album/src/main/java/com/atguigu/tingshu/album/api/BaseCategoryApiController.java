package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
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

}

