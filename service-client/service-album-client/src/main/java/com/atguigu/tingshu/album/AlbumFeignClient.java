package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
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


    /**
     * 查询用户未购买声音列表：内部接口，供订单服务调用
     * @param trackId 声音ID
     * @param trackCount 当前用户要购买的声音数量
     * @return List<TrackInfo> 从trackid的序号开始，获取trackCount个用户未购买的声音
     */
    @GetMapping("/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> findWaitBuyTrackInfoList(@PathVariable Long trackId, @PathVariable Integer trackCount);

    /**
     * 根据声音ID查询声音信息
     * @param id 声音ID
     * @return 声音信息
     */
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    // 这里直接掉的 mp 的服务层方法，所以缓存注解只能加到改控制器上
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id);
}
