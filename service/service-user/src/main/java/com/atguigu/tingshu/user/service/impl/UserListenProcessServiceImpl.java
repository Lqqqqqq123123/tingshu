package com.atguigu.tingshu.user.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
        // 拼接当前用户的播放进度的集合名称
        String collectionName = this.getCollectionName(userId);
        Query query = new Query();
        query.addCriteria(Criteria.where("trackId").is(trackId));
        UserListenProcess resp = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        if(resp != null){
            return resp.getBreakSecond();
        }
        return BigDecimal.valueOf(0);
    }

    /**
     * 更新声音播放进度
     * @param userListenProcessVo 声音播放进度信息
     * @return
     */
    // todo:这里的前端是定时任务,后续要改为非定时任务,也就是只在退出声音时才去调用这个接口,同时通过 rmq 去更新当前声音和专辑的统计信息(只更新播放量)
    @Override
    public void updateUserListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
        // 1. 获取当前用户的播放进度集合名称
        String colletionName = this.getCollectionName(userId);

        // 2. 创建查询条件 : 声音 id
        Query query = new Query();
        query.addCriteria(Criteria.where("trackId").is(userListenProcessVo.getTrackId()));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, colletionName);

        // 前端传来的秒还带小数点: 3.12132123s，这里只需要秒
        double breakSecond = Math.round(Double.parseDouble(userListenProcessVo.getBreakSecond().toString()));
        System.out.println("breakSecond:"+breakSecond);

        // 3. 如果存在,更新
        if(userListenProcess != null){
            // 前端传来的秒还带小数点: 3.12132123s，这里只需要秒
            userListenProcess.setUpdateTime(new Date());
            userListenProcess.setBreakSecond(BigDecimal.valueOf(breakSecond));
        }
        // 4. 不存在,插入
        else{
            userListenProcess = new UserListenProcess();
            userListenProcess.setTrackId(userListenProcessVo.getTrackId());
            userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
            userListenProcess.setBreakSecond(BigDecimal.valueOf(breakSecond));
        }

        mongoTemplate.save(userListenProcess, colletionName);

        // 5 todo: 更新播放统计数值(mysql以及 es)
        /**
         * 生产者当前每隔10s调用一次 业务要求:在当天该用户只能增加一次播放量:防止刷播放量
         * 实现方式
         * 采用 用户id:专辑id:播放量id来做本次 rmq 业务的唯一标识,保证幂等性
         */
        // 5.1 获取 key(唯一标识)
        String key = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX +userId + "_" + userListenProcessVo.getAlbumId() + "_" + userListenProcessVo.getTrackId();
        // 5.2 过期时间计算
        DateTime end = DateUtil.endOfDay(new Date());
        Long expireTime = end.getTime() - System.currentTimeMillis();

        // 5.3 采用 set nx
        Boolean isSuccess =  redisTemplate.opsForValue().setIfAbsent(key, 1, expireTime, TimeUnit.MILLISECONDS);
        // Boolean isSuccess =  redisTemplate.opsForValue().setIfAbsent(key, 1, 30, TimeUnit.SECONDS); // todo : 暂时设置为30s过期,便于调试,之后改为一天

        // 5.4 如果没有成功,则说明消息已经处理过了,直接返回,保证幂等性
        if(!isSuccess){
            return;
        }
        // 5.5 否则处理业务逻辑:向消费者发送消息
        // 5.5.1 创建更新统计数值信息的 vo(如果发送的消息是一个自定义对象,则必须实现Serializable接口)
        TrackStatMqVo vo = new TrackStatMqVo();
        vo.setBusinessNo(IdUtil.randomUUID());
        vo.setAlbumId(userListenProcessVo.getAlbumId());
        vo.setTrackId(userListenProcessVo.getTrackId());
        vo.setStatType(SystemConstant.TRACK_STAT_PLAY);
        vo.setCount(1);

        // 5.5.2 发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_TRACK, MqConst.ROUTING_TRACK_STAT_UPDATE,  vo);

    }


    /**
     * 获取当前用户最近播放声音
     * @return {albumId:1,trackId:12}
     */
    @Override
    public Map<String, Long> getLatelyTrack(Long userId) {
        // 直接去 mongodb 中查询当前用户的播放进度,然后按照时间倒序排序,取第一个

        // 1. 获取当前用户的播放进度集合名称
        String collectionName = this.getCollectionName(userId);

        // 2. 降序排序
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
        query.limit(1);

        // 3. 查询
        List<UserListenProcess> userListenProcesses = mongoTemplate.find(query, UserListenProcess.class, collectionName);

        if(CollectionUtil.isNotEmpty(userListenProcesses)){
            UserListenProcess userListenProcess = userListenProcesses.get(0);
            return Map.of("albumId", userListenProcess.getAlbumId(), "trackId", userListenProcess.getTrackId());
        }

        return Collections.emptyMap();
    }


    /**
     * 获取当前用户的播放进度集合名称
     * @param userId 用户ID
     * @return 集合名称
     */

    public String getCollectionName(Long userId) {
        return MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
    }
}
