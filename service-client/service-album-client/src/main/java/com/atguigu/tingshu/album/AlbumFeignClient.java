package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
}
