package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserVipService;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import com.atguigu.tingshu.user.strategy.DeliveryStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Vip发放策略
 * @author liutianba7
 * @create 2025/12/23 09:49
 */
@Slf4j
@Component("1003")
public class VipStrategy implements DeliveryStrategy {
    @Autowired
    private UserVipServiceMapper userVipServiceMapper;
    @Autowired
    private VipServiceConfigService vipServiceConfigService;
    @Autowired
    private UserInfoService userInfoService;
    @Override
    public void delivery(UserPaidRecordVo vo) {
        log.info("liutianba7:VIP 发放策略");
        // 1. 看看是否已经有了会员的支付记录
        LambdaQueryWrapper<UserVipService> wr = new LambdaQueryWrapper<>();
        wr.eq(UserVipService::getOrderNo, vo.getOrderNo()).select(UserVipService::getId);
        Long count = userVipServiceMapper.selectCount(wr);

        if(count > 0){
            return ;
        }

        // 2. 先拿到当前用户信息以及购买的会员套餐信息
        UserInfo userinfo = userInfoService.getById(vo.getUserId());
        VipServiceConfig vipServiceConfig = vipServiceConfigService.getById(vo.getItemIdList().get(0));

        // 3. 新增会员购买记录
        UserVipService userVipService = new UserVipService();
        userVipService.setOrderNo(vo.getOrderNo());
        userVipService.setUserId(vo.getUserId());
        // 生效时间:之前如果是会员，且过期时间 > 当前时间，则生效时间是过期时间，否则生效时间是当前时间
        if(userinfo.getIsVip() == 1 && userinfo.getVipExpireTime().after(new Date())){
            // 如果是 vip 且 vip 没过期，则生效时间是过期时间
            // userVipService.setStartTime(DateUtil.offsetDay(userinfo.getVipExpireTime(), 1));
            userVipService.setStartTime(userinfo.getVipExpireTime());
        }else{
            // 如果不是 vip 则生效时间是当前时间
            userVipService.setStartTime(new Date());
        }

        // 过期时间：在生效时间的基础上 + 购买的会员套餐的时间
        Integer serviceMonth = vipServiceConfig.getServiceMonth(); // 服务月数
        // a. 将 Date 转换为 LocalDateTime (更易于操作)
        LocalDateTime start = userVipService.getStartTime().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        // b. 过期时间
        LocalDateTime end = start.plusMonths(serviceMonth);
        // c. 将计算后的 LocalDateTime 转回 Date
        Date expireTime = Date.from(end.atZone(ZoneId.systemDefault()).toInstant());
        userVipService.setExpireTime(expireTime);

        // d. 将会员购买信息保存至数据库
        userVipServiceMapper.insert(userVipService);

        // 4 更新用户信息
        userinfo.setIsVip(1);
        userinfo.setVipExpireTime(expireTime);
        userInfoService.updateById(userinfo);
    }
}
