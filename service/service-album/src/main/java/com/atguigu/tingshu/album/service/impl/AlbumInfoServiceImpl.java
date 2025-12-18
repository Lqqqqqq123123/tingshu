package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.common.cache.RedisCache;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.atguigu.tingshu.common.constant.SystemConstant.ALBUM_PAY_TYPE_REQUIRE;
import static com.atguigu.tingshu.common.constant.SystemConstant.ALBUM_PAY_TYPE_VIPFREE;

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

    @Autowired
    private AuditService auditService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savaAlbumInfo(AlbumInfoVo vo, Long userId) {

        // 1. 保存专辑信息 album_info
        // 后端封装的的字段：用户id， 创建时间， 状态

        // 1.1 vo --> po
        AlbumInfo po = BeanUtil.copyProperties(vo, AlbumInfo.class);
        String payType = vo.getPayType();
        // 1.2封装其他字段
        po.setUserId(userId);
        if (ALBUM_PAY_TYPE_VIPFREE.equals(payType) || ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
            //只需要对VIP免费或付费资源设置试听集
            po.setTracksForFree(SystemConstant.TRACKS_FOR_FREE_NUM);
        }
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

        // 4.对专辑中文本内容进行审核  & 索引库ES中新增记录
        String text = po.getAlbumTitle() + po.getAlbumIntro();

        String suggestion = auditService.auditText(text);

        if ("pass".equals(suggestion)) {
            po.setStatus(SystemConstant.ALBUM_STATUS_PASS);
            //TODO 发送MQ消息 通知 搜索服务 将专辑存入ES引库
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, po.getId());
        } else if ("review".equals(suggestion)) {
            po.setStatus(SystemConstant.ALBUM_STATUS_REVIEW);
        } else if ("block".equals(suggestion)) {
            po.setStatus(SystemConstant.ALBUM_STATUS_NO_PASS);
        }
        albumInfoMapper.updateById(po);

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


        // todo : 通知search微服务中的消费者，删除专辑索引
        rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, id);
    }



    /**
     * 根据 id 查询专辑信息，优先从 redis 中查询，如果 redis 中没有，才去调用 getAlbumInfoFromDB 从 数据库查询并写入到 redis中
     * @param id 专辑 id
     * @return 专辑信息
     */
    @Override
    @RedisCache(prefix = "album:albuminfo:", timeout = 3600)
    public AlbumInfo getAlbumInfo(Long id) {
//        try {
//            // 1. 先从缓存中查询专辑信息
//            // 1.1 构建缓存key
//            String albumInfoKey = RedisConstant.ALBUM_INFO_PREFIX + id;
//            // 1.2 获取缓存数据
//            AlbumInfo albumInfo = (AlbumInfo) redisTemplate.opsForValue().get(albumInfoKey);
//            // 1.3 命中：返回数据
//            if (albumInfo != null) {
//                log.info("liutianba7：命中缓存：{}", albumInfoKey);
//                 return albumInfo;
//            }
//
//            // 2. 未命中：获取分布式锁
//            // 2.1 构建锁的key
//            String lockKey = albumInfoKey + RedisConstant.CACHE_LOCK_SUFFIX;
//
//            // 2.2 获取锁实例
//            RLock lock = redissonClient.getLock(lockKey);
//
//            // 2.3 尝试获取分布式锁
//            boolean is_locked = lock.tryLock(RedisConstant.ALBUM_LOCK_WAIT_PX1, RedisConstant.ALBUM_LOCK_EXPIRE_PX2, TimeUnit.SECONDS);
//
//            // 3. 获取锁成功：从数据库查数据，然后写入缓存，返回数据
//            if(is_locked){
//                try {
//                    // 3.1 从数据库查询专辑信息
//                    AlbumInfo albuminfo = this.getAlbumInfoFromDB(id);
//                    // 3.2 写入缓存 为了解决缓存雪崩：缓存的过期时间要在基础值上 + 一个随机值
//                    redisTemplate.opsForValue().set(albumInfoKey, albuminfo, RedisConstant.ALBUM_TIMEOUT + new Random().nextInt(100), TimeUnit.SECONDS); // 一小时的过期时间 + 一个随机值
//                    // 3.4 返回数据
//                    return albuminfo;
//                } finally {
//                    // 3.3 释放锁
//                    lock.unlock();
//                }
//
//            }
//            else{
//                // 4. 获取锁失败：说明 waitTime 时间到了还没拿到锁
//                // 此时可以再次尝试调用本方法（递归），或者直接报错/降级
//                // 通常由于 tryLock 内部已经等了很久了，再查一次缓存即可
//                Thread.sleep(200); // 这里的 sleep 是为了避免极端情况下的栈溢出重试
//                return getAlbumInfo(id);
//            }
//        } catch (InterruptedException e) {
//
//            log.info("liutianba: getAlbumInfo 方法出现异常");
//            // 5. 兜底：redis宕机，直接从数据库查询
//            return this.getAlbumInfoFromDB(id);
//        }
        return this.getAlbumInfoFromDB(id);
    }

    /**
     * 根据 id 从数据库查询专辑信息
     * @param id 专辑id
     * @return 专辑信息
     */
    @Override
    public AlbumInfo getAlbumInfoFromDB(Long id) {
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
        if(!CollectionUtils.isEmpty(tlist)){
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

        // 4. 再次对专辑内容进行审核（同步审核）
        String text = po.getAlbumTitle() + po.getAlbumIntro();

        String suggestion = auditService.auditText(text);

        if ("pass".equals(suggestion)) {
            po.setStatus(SystemConstant.ALBUM_STATUS_PASS);
            //TODO 发送MQ消息 通知 搜索服务 将专辑存入ES引库
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, po.getId());
        } else if ("review".equals(suggestion)) {
            po.setStatus(SystemConstant.ALBUM_STATUS_REVIEW);
        } else if ("block".equals(suggestion)) {
            po.setStatus(SystemConstant.ALBUM_STATUS_NO_PASS);
        }
        albumInfoMapper.updateById(po);

    }

    /**
     * 根据专辑ID查询专辑统计信息
     * @param albumId
     * @return
     */
    @Override
    @RedisCache(prefix = "album:albumstat:", timeout = 3600)
    public AlbumStatVo getAlbumStatVo(Long albumId) {
        return albumInfoMapper.getAlbumStatVo(albumId);
    }
}
