package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
            message.put("title", "首次使用，赠送余额100元");

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
     *
     * @return Vo
     */
    @Override
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
}
