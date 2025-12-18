package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 专辑模块远程调用Feign接口
 * @author liutianba
 * @create 2025/12/13 08:55
 */
@FeignClient(value = "service-album", fallback = AlbumDegradeFeignClient.class, path = "/api/album")
public interface AlbumFeignClient {

    /**
     * 获取专辑信息，包含专辑属性列表
     * @param id
     * @return
     */
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id);


    /**
     * 根据三级分类ID查询分类视图
     * @param category3Id
     * @return
     */
    @GetMapping("/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id);

    /**
     * 根据一级分类Id查询置顶7个三级分类列表
     * @param category1Id
     * @return
     */
    @GetMapping("/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> findTopBaseCategory3(@PathVariable Long category1Id);


    /**
     * 根据专辑ID查询专辑统计信息
     * @param albumId
     * @return
     */
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId);

    /**
     * 查询所有的一级分类 id 与 名称
     * @return [{}, {} ... ]
     */
    @GetMapping("/category/findAllCategory1")
    public Result<List<BaseCategory1>> findAllCategory1();
}
