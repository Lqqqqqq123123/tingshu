package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {

    /**
     *
     * @param vo 专辑的 vo 信息
     * @param userId 当前用户的 id
     */
    void savaAlbumInfo(AlbumInfoVo vo, Long userId);


    /**
     * 保存专辑的统计信息
     * @param albumId 专辑id
     * @param statType 统计类型
     * @param statNum 统计数量
     */
    public void saveAlbumInfoStat(Long albumId, String statType, int statNum);

    /**
     * 查询用户专辑列表
     * @param res 分页对象
     * @param query 查询条件
     * @return
     */
    IPage<AlbumListVo> findUserAlbumPage(IPage<AlbumListVo> res, AlbumInfoQuery query);

    /**
     * 根据 id 删除专辑
     * @param id
     */
    void removeAlbumInfo(Long id);

    /**
     * 根据 id 查询专辑信息
     * @param id
     * @return
     */
    AlbumInfo getAlbumInfo(Long id);

    /**
     * 修改专辑信息
     * @param id
     * @param vo
     */
    void updateAlbumInfo(Long id, AlbumInfoVo vo);
}
