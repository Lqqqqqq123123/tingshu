package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    IPage<AlbumListVo> findUserAlbumPage(IPage<AlbumListVo> res, @Param("query") AlbumInfoQuery query);

    /**
     * 根据专辑ID查询专辑统计信息
     * @param albumId
     * @return
     */
    AlbumStatVo getAlbumStatVo(@Param("albumId") Long albumId);
}
