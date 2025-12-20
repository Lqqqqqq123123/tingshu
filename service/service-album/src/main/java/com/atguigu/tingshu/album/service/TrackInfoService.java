package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

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

    /**
     * 基于用户选择的声音，动态的获取为购买的声音数量，得到声音购买列表
     * @param trackId 当前选择的声音ID
     * @param userId 当前用户ID
     * @return List<Map<String, Object>>
     */

    List<Map<String, Object>> findUserTrackPaidList(Long trackId, Long userId);

    /**
     * 查询用户未购买声音列表：内部接口，供订单服务调用
     * @param trackId 声音ID
     * @param userId 用户ID
     * @param trackCount 当前用户要购买的声音数量
     * @return List<TrackInfo> 从trackid的序号开始，获取trackCount个用户未购买的声音
     */
    List<TrackInfo> findWaitBuyTrackInfoList(Long userId, Long trackId, Integer trackCount);
}
