package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TrackInfoService extends IService<TrackInfo> {



    /**
     * 保存音频信息
     * @param trackInfoVo
     * @param userId
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId);


    /**
     * 保存声音统计信息
     * @param trackId 声音ID
     * @param statType 统计类型
     * @param statNum 统计数值
     */
    void saveTrackStat(Long trackId, String statType, int statNum);

    /**
     * 条件分页查询当前用户声音列表（包含声音统计信息）
     *
     * @param pageInfo 分页对象
     * @param trackInfoQuery 查询条件（用户ID，关键字，审核状态）
     * @return
     */
    Page<TrackListVo> getUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery);

    /**
     * 修改声音信息
     * @param trackInfo
     */
    void updateTrackInfo(TrackInfo trackInfo);

    /**
     * 删除声音记录
     * @param id
     * @return
     */
    void removeTrackInfo(Long id);

    /**
     * 需求：用户未登录，可以给用户展示声音列表；用户已登录，可以给用户展示声音列表，并动态渲染付费标识
     * 分页查询专辑下声音列表（动态渲染付费标识）
     * @param pageInfo 分页参数
     * @param albumId 专辑id
     * @param userId 用户id
     * @return
     */
    IPage<AlbumTrackListVo> findAlbumTrackPage(IPage<AlbumTrackListVo> pageInfo, Long albumId, Long userId);

    /**
     * 查询声音统计信息
     * @param trackId
     * @return TrackStatVo
     */
    TrackStatVo getTrackStatVo(Long trackId);

    /**
     * 更新声音 + 对应专辑的统计信息
     * @param vo
     */
    void updateStat(TrackStatMqVo vo);
}
