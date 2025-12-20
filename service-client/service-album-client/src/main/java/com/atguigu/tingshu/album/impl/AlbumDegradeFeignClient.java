package com.atguigu.tingshu.album.impl;


import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AlbumDegradeFeignClient implements AlbumFeignClient {


    /**
     * 获取专辑信息，包含专辑列表
     * @param id
     * @return
     */
    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
      log.info("[远程调用专辑服务]的 [getAlbumInfo] 接口出现异常，降级处理");
      return Result.fail();
    }

    @Override
    public Result<BaseCategoryView> getCategoryView(Long category3Id) {
        log.info("[远程调用专辑服务]的 [getCategoryView] 接口出现异常，降级处理");
        return Result.fail();
    }

    @Override
    public Result<List<BaseCategory3>> findTopBaseCategory3(Long category1Id) {
        log.info("[远程调用专辑服务]的 [findTopBaseCategory3] 接口出现异常，降级处理");
        return Result.fail();
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.info("[远程调用专辑服务]的 [getAlbumStatVo] 接口出现异常，降级处理");
        return Result.fail();
    }

    @Override
    public Result<List<BaseCategory1>> findAllCategory1() {
        log.info("[远程调用专辑服务]的 [findAllCategory1] 接口出现异常，降级处理");
        return Result.fail();
    }

    @Override
    public Result<List<TrackInfo>> findWaitBuyTrackInfoList(Long trackId, Integer trackCount) {
        log.info("[远程调用专辑服务]的 [findWaitBuyTrackInfoList] 接口出现异常，降级处理");
        return null;
    }
}



