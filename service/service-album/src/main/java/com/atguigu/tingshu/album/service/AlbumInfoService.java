package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

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
     * 修改专辑信息
     * @param id
     * @param vo
     */
    void updateAlbumInfo(Long id, AlbumInfoVo vo);


    /**
     * 根据专辑ID查询专辑统计信息
     * @param albumId
     * @return
     */
    AlbumStatVo getAlbumStatVo(Long albumId);


    /**
     * 根据 id 从数据库查询专辑信息
     * @param id 专辑id
     * @return 专辑信息
     */
    AlbumInfo getAlbumInfoFromDB(Long id);

    /**
     * 根据 id 查询专辑信息，优先从 redis 中查询，如果 redis 中没有，才去调用 getAlbumInfoFromDB 从 数据库查询并写入到 redis中
     * @param id 专辑 id
     * @return 专辑信息
     */
    AlbumInfo getAlbumInfo(Long id);

    /**
     * 批量将数据库存在的过审的专辑存到布隆过滤器中
     */
    public void batchUpperToBloom();
}
