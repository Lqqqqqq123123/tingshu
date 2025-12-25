package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.cache.RedisCache;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.UserPaidAlbumService;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import com.atguigu.tingshu.user.strategy.DeliveryStrategy;
import com.atguigu.tingshu.user.strategy.factory.DeliveryStrategyFactory;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

	@Autowired
	private UserInfoMapper userInfoMapper;

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserPaidAlbumService userPaidAlbumService;

    @Autowired
    private UserPaidTrackService userPaidTrackService;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private VipServiceConfigService vipServiceConfigService;



    /**
     * 实现微信登录
     * @param code
     * @return map: {token:token}
     * @throws WxErrorException
     */
    @Override
    public Map<String, String> wxLogin(String code) throws WxErrorException {

        // 1. 根据 code 对接微信微服务，获取用户唯一表示 open_id
        WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        String openId = sessionInfo.getOpenid();
        log.info("openId: {}", openId);

        // 2. 去用户表看一看是否已经存在该用户
        UserInfo userInfo = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getWxOpenId, openId)
        );

        // 3 不存在就要去新增用户

        // 3.1 创建用户
        if(userInfo == null){
            UserInfo info = new UserInfo();
            info.setWxOpenId(openId);
            info.setAvatarUrl("http://115.190.231.171:9000/tingshu/2025/12/09/2.png");// 默认用户头像地址：http://115.190.231.171:9000/tingshu/2025/12/09/2.png
            info.setNickname("听友" + UUID.randomUUID().toString());

            userInfoMapper.insert(info);
            // 3.2 todo: 通过rmq异步调用账户服务，为改用户初始化账户信息
            // 3.2.1 初始化mq消息
            HashMap<String, Object> message = new HashMap<>();
            message.put("userId", info.getId());
            message.put("amount", 1000);
            message.put("orderNo", "free_amount" + DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId());
            message.put("title", "首次使用，赠送余额1000元");

            rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, message);
        }

        // 4. 返回当前用户的 openId;

        // 4.1 生成随机令牌
        String token = UUID.randomUUID().toString();

        // 4.2 将用户基本信息保存到redis
        String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        UserInfoVo vo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);

        redisTemplate.opsForValue().set(loginKey, vo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);

        return Map.of("token",  token);
    }

    /**
     * 获取当前登录用户基本信息
     * @return Vo
     */
    @Override
    @RedisCache(prefix = "user:userInfoVo:", timeout = 3600)
    public UserInfoVo getUserInfo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        return BeanUtil.copyProperties(userInfo, UserInfoVo.class);
    }

    /**
     * 更新当前用户信息 :只允许修改昵称和头像
     * @param userInfoVo
     * @return
     */
    @Override
    public void updateUser(UserInfoVo userInfoVo) {
        // 获取用户id
        Long userid = AuthContextHolder.getUserId();
        UserInfo userInfo = BeanUtil.copyProperties(userInfoVo, UserInfo.class);
        userInfo.setId(userid);
        userInfoMapper.updateById(userInfo);

    }

    /**
     * 提交需要检查购买状态声音ID列表，响应每个声音购买状态
     * @param userId
     * @param albumId
     * @param ids 待检查购买状态声音ID列表（已经过滤了免费试听声音id）
     * @return [声音ID，支付状态] 0 未购买 1 已购买
     */
    @Override
    public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> ids) {

        // 1 根据专辑id + userid 直接去查当前用户是否购买了专辑

        // 1.1 包装查询条件
        LambdaQueryWrapper<UserPaidAlbum> wr1 = new LambdaQueryWrapper<>();
        wr1.eq(UserPaidAlbum::getUserId, userId);
        wr1.eq(UserPaidAlbum::getAlbumId, albumId);

        // 1.2 查询，获取行数
        long count = userPaidAlbumService.count(wr1);

        // 1.3 如果行数大于0，则说明当前用户已经购买了专辑，那么我们直接将这些待检查的声音的支付状态设置为已购买，然后直接返回
        if(count > 0){
            return ids.stream().collect(Collectors.toMap(k -> k, v -> 1));
        }


        // 2 否则，我们根据声音ID列表，去数据库中查询这些声音的支付状态（也就是根据 userId + albumId 去查询当前用户购买了那些声音）
        LambdaQueryWrapper<UserPaidTrack> wr2 = new LambdaQueryWrapper<>();
        wr2.eq(UserPaidTrack::getUserId, userId);
        wr2.eq(UserPaidTrack::getAlbumId, albumId);

        List<UserPaidTrack> list = userPaidTrackService.list(wr2);

        // 3.1 如果不存在购买的声音，那么我们将这些声音的支付状态设置为0，然后直接返回
        if(CollectionUtil.isEmpty(list)){
            return ids.stream().collect(Collectors.toMap(k -> k, v -> 0));
        }

        // 3.2 否则，将2查出来的用户购买了的声音的支付状态设置为1， 其余为0，然后响应
        // 3.2.1 将查询来的购买声音的id转为set
        Set<Long> buy_set = list.stream()
                .map(UserPaidTrack::getTrackId)
                .collect(Collectors.toSet());
        return ids.stream().collect(
                Collectors.toMap(k -> k, v -> buy_set.contains(v) ? 1 : 0)
        );
    }

    /**
     * 判断当前用户是否购买过指定专辑
     * @param albumId
     * @return 0 未购买 1 已购买
     */
    @Override
    public Boolean isPaidAlbum(Long userId, Long albumId) {
        // 1 根据专辑id + userid 直接去查当前用户是否购买了专辑
        LambdaQueryWrapper<UserPaidAlbum> wr1 = new LambdaQueryWrapper<>();
        wr1.eq(UserPaidAlbum::getUserId, userId).eq(UserPaidAlbum::getAlbumId, albumId);
        return userPaidAlbumService.count(wr1) > 0;
    }

    /**
     * 查询用户已购买的声音ID列表
     * @param albumId 专辑id
     * @param userId 用户id
     * @return 在该专辑下已经购买的声音ID列表
     */
    @Override
    public List<Long> findUserPaidTrackList(Long userId, Long albumId) {
        LambdaQueryWrapper<UserPaidTrack> wr = new LambdaQueryWrapper<>();
        wr.eq(UserPaidTrack::getUserId, userId).eq(UserPaidTrack::getAlbumId, albumId).select(UserPaidTrack::getTrackId);
        List<UserPaidTrack> list = userPaidTrackService.list(wr);
        if(!CollectionUtil.isEmpty(list)){
            return list
                    .stream()
                    .map(UserPaidTrack::getTrackId)
                    .toList();
        }
        return null;
    }

    @Autowired
    private DeliveryStrategyFactory buyStrategyFactory;

    /**
     * 用户支付成功后，虚拟物品发货 内部接口：订单服务调用
     * todo : 使用策略者模式优化该方法
     * @param vo 虚拟物品信息
     * @return 虚拟物品发货结果
     */
    @Override
    public void savePaidRecord(UserPaidRecordVo vo) {
        // 1. 去判断当前用户购买的商品类型，从而走不同的逻辑处理
        String itemType = vo.getItemType();

        // 2. 创建策略者，根据itemtype获得不同的实现类
        DeliveryStrategy deliveryStrategy = buyStrategyFactory.getDeliveryStrategy(itemType);
        deliveryStrategy.delivery(vo);

        // 策略者模式优化了代码结构
//        // 1.1 专辑
//        if(SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)){
//            // 1.1.1 看看是否已经有了专辑的支付记录
//            LambdaQueryWrapper<UserPaidAlbum> wr = new LambdaQueryWrapper<>();
//            wr.eq(UserPaidAlbum::getOrderNo, vo.getOrderNo()).select(UserPaidAlbum::getId); // 索引覆盖，避免回表查询
//            Long count = userPaidAlbumService.count(wr);
//            if(count > 0){
//                return;
//            }
//            // 1.1.2 保存专辑支付记录
//            UserPaidAlbum paidAlbum = new UserPaidAlbum();
//            paidAlbum.setOrderNo(vo.getOrderNo());
//            paidAlbum.setAlbumId(vo.getItemIdList().get(0));
//            paidAlbum.setUserId(vo.getUserId());
//            userPaidAlbumService.save(paidAlbum);
//        }
//        // 1.2 声音
//        else if(SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType))
//        {
//            // 1.2.1 看看是否已经有了声音的支付记录
//            LambdaQueryWrapper<UserPaidTrack> wr = new LambdaQueryWrapper<>();
//            wr.eq(UserPaidTrack::getOrderNo, vo.getOrderNo()).select(UserPaidTrack::getId); // 索引覆盖，避免回表查询
//            Long count = userPaidTrackService.count(wr);
//            if(count > 0){
//                return;
//            }
//            // 1.2.2 查询当前一条声音的信息，拿到专辑ID
//            TrackInfo data = albumFeignClient.getTrackInfo(vo.getItemIdList().get(0)).getData();
//            Assert.notNull(data, "声音不存在");
//
//            // 1.2.2 批量保存声音支付记录
//            List<UserPaidTrack> list = vo.getItemIdList()
//                    .stream()
//                    .map(itemId -> {
//                        UserPaidTrack paidTrack = new UserPaidTrack();
//                        paidTrack.setOrderNo(vo.getOrderNo());
//                        paidTrack.setTrackId(itemId);
//                        paidTrack.setUserId(vo.getUserId());
//                        paidTrack.setAlbumId(data.getAlbumId());
//                        return paidTrack;
//                    }).toList();
//            userPaidTrackService.saveBatch(list);
//        }
//        // 1.3 会员
//        else if(SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)){
//            // 1.3.1 看看是否已经有了会员的支付记录
//            LambdaQueryWrapper<UserVipService> wr = new LambdaQueryWrapper<>();
//            wr.eq(UserVipService::getOrderNo, vo.getOrderNo()).select(UserVipService::getId);
//            Long count = userVipServiceMapper.selectCount(wr);
//
//            if(count > 0){
//                return ;
//            }
//
//            // 1.3.2 先拿到当前用户信息以及购买的会员套餐信息
//            UserInfo userinfo = this.getById(vo.getUserId());
//            VipServiceConfig vipServiceConfig = vipServiceConfigService.getById(vo.getItemIdList().get(0));
//
//            // 1.3.3 新增会员购买记录
//            UserVipService userVipService = new UserVipService();
//            userVipService.setOrderNo(vo.getOrderNo());
//            userVipService.setUserId(vo.getUserId());
//            // 生效时间:之前如果是会员，且过期时间 > 当前时间，则生效时间是过期时间，否则生效时间是当前时间
//            if(userinfo.getIsVip() == 1 && userinfo.getVipExpireTime().after(new Date())){
//                // 如果是 vip 且 vip 没过期，则生效时间是过期时间
//                // userVipService.setStartTime(DateUtil.offsetDay(userinfo.getVipExpireTime(), 1));
//                userVipService.setStartTime(userinfo.getVipExpireTime());
//            }else{
//                // 如果不是 vip 则生效时间是当前时间
//                userVipService.setStartTime(new Date());
//            }
//
//            // 过期时间：在生效时间的基础上 + 购买的会员套餐的时间
//            Integer serviceMonth = vipServiceConfig.getServiceMonth(); // 服务月数
//            // a. 将 Date 转换为 LocalDateTime (更易于操作)
//            LocalDateTime start = userVipService.getStartTime().toInstant()
//                    .atZone(ZoneId.systemDefault())
//                    .toLocalDateTime();
//            // b. 过期时间
//            LocalDateTime end = start.plusMonths(serviceMonth);
//            // c. 将计算后的 LocalDateTime 转回 Date
//            Date expireTime = Date.from(end.atZone(ZoneId.systemDefault()).toInstant());
//            userVipService.setExpireTime(expireTime);
//
//            // d. 将会员购买信息保存至数据库
//            userVipServiceMapper.insert(userVipService);
//
//            // 1.3.4 更新用户信息
//            userinfo.setIsVip(1);
//            userinfo.setVipExpireTime(expireTime);
//            this.updateById(userinfo);
//        }
    }

    @Override
    public void resetVip(Date date) {
        // 1. 修改所有是 vip，并且过期的会员的标识
        LambdaUpdateWrapper<UserInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserInfo::getIsVip, 1)
                .le(UserInfo::getVipExpireTime, date)
                .or()
                .isNull(UserInfo::getVipExpireTime)
                .set(UserInfo::getIsVip, 0);

        this.update(updateWrapper);
    }
}
