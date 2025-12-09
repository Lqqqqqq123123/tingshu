package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savaAlbumInfo(AlbumInfoVo vo, Long userId) {

        // 1. 保存专辑信息 album_info
        // 后端封装的的字段：用户id， 创建时间， 状态

        // 1.1 vo --> po
        AlbumInfo po = BeanUtil.copyProperties(vo, AlbumInfo.class);

        // 1.2封装其他字段
        po.setUserId(userId);
        po.setTracksForFree(5);
        po.setStatus(SystemConstant.ALBUM_STATUS_NO_PASS);

        albumInfoMapper.insert(po); // 保存专辑信息，回显主键

        // 2. 保存专辑与属性值的关联关系 album_attribute_value

        // notes：可能有多个属性值，也可能没有属性值，所以需要判断
        if(vo.getAlbumAttributeValueVoList() == null || vo.getAlbumAttributeValueVoList().size() == 0){
           log.info("当前专辑没有属性值");
        }else{
           List<AlbumAttributeValue> list = vo.getAlbumAttributeValueVoList()
                   .stream()
                   .map(t ->{
                       AlbumAttributeValue aav_po = BeanUtil.copyProperties(t, AlbumAttributeValue.class);
                       aav_po.setAlbumId(po.getId());
                       return aav_po;
                   })
                   .toList();
           // 批量保存
            albumAttributeValueService.saveBatch(list);
        }

        // 3. 保存专辑的统计信息 album_stat (初始值为都为0）
        saveAlbumInfoStat(po.getId(), "0401", 0);
        saveAlbumInfoStat(po.getId(), "0402", 0);
        saveAlbumInfoStat(po.getId(), "0403", 0);
        saveAlbumInfoStat(po.getId(), "0404", 0);

        // 4. todo 对专辑中文本内容进行审核  & 索引库ES中新增记录

    }

    /**
     * 保存专辑统计信息
     *
     * @param albumId  专辑ID
     * @param statType 统计类型
     * @param statNum  统计数值 0401-播放量 0402-订阅量 0403-购买量 0403-评论数'
     */
    @Override
    public void saveAlbumInfoStat(Long albumId, String statType, int statNum) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(statNum);
        albumStatMapper.insert(albumStat);
    }

    @Override
    public IPage<AlbumListVo> findUserAlbumPage(IPage<AlbumListVo> res, AlbumInfoQuery query) {
        return albumInfoMapper.findUserAlbumPage(res, query);
    }

    @Override
    public void removeAlbumInfo(Long id) {
        // 1. 删除专辑信息 album_info
        albumInfoMapper.deleteById(id);
        // 2. 删除专辑属性值关联表中的信息 album_attribute_value
        LambdaQueryWrapper<AlbumAttributeValue> wr1 = new LambdaQueryWrapper<>();
        wr1.eq(AlbumAttributeValue::getAlbumId, id);
        albumAttributeValueService.remove(wr1);
        // 3. 删除专辑统计信息 album_stat
        LambdaQueryWrapper<AlbumStat> wr2 = new LambdaQueryWrapper<>();
        wr2.eq(AlbumStat::getAlbumId, id);
        albumStatMapper.delete(wr2);
    }

    @Override
    public AlbumInfo getAlbumInfo(Long id) {
        // 1. 查询专辑信息 album_info
        AlbumInfo albumInfo = albumInfoMapper.selectById(id);

        // 2. 查询专辑属性值 album_attribute_value
        List<AlbumAttributeValue>  list = albumAttributeValueService.list(
                new LambdaQueryWrapper<AlbumAttributeValue>()
                        .eq(AlbumAttributeValue::getAlbumId, id)
        );

        albumInfo.setAlbumAttributeValueVoList(list);
        return albumInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(Long id, AlbumInfoVo vo) {
        // 1. 更新专辑信息 album_info
        AlbumInfo po = BeanUtil.copyProperties(vo, AlbumInfo.class);
        po.setId(id);
        po.setStatus(SystemConstant.ALBUM_STATUS_NO_PASS);
        albumInfoMapper.updateById(po);


        // 2. 对于中间表，先删除、在添加
        albumAttributeValueService.remove(
                new LambdaQueryWrapper<AlbumAttributeValue>()
                        .eq(AlbumAttributeValue::getAlbumId, id)
        );

        List<AlbumAttributeValueVo> tlist = vo.getAlbumAttributeValueVoList();
        if(tlist == null || tlist.size() == 0){
            List<AlbumAttributeValue> list = tlist
                    .stream()
                    .map(t ->{
                        AlbumAttributeValue aav_po = BeanUtil.copyProperties(t, AlbumAttributeValue.class);
                        aav_po.setAlbumId(id);
                        return aav_po;
                    })
                    .toList();

            albumAttributeValueService.saveBatch(list);
        }


        // 3. 对于统计信息表,不用修改

        // 4. todo：再次对专辑内容进行审核


    }
}
