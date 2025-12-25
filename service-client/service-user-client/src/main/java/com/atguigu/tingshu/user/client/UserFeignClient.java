package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.impl.UserDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-user", fallback = UserDegradeFeignClient.class, path = "/api/user")
public interface UserFeignClient {

    /**
     * 根据用户ID查询用户/主播基本信息
     * @param userId
     * @return
     */
    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId);

    /**
     * 提交需要检查购买状态声音ID列表，响应每个声音购买状态
     * @param userId
     * @param albumId
     * @param ids 待检查购买状态声音ID列表
     * @return [声音id:购买状态] 0 未购买 1 已购买
     */
    @PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(
            @PathVariable(value = "userId") Long userId,
            @PathVariable(value = "albumId") Long albumId,
            @RequestBody List<Long> ids
    );


    /**
     * 根据ID查询VIP服务配置，该接口被订单微服务访问，需提供远程接口
     * @param id VIP服务配置ID
     * @return VIP服务配置
     */
    @GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")
    public Result<VipServiceConfig> getVipServiceConfigById(@PathVariable("id") Long id);


    /**
     * 判断当前用户是否购买过指定专辑
     * @param albumId
     * @return 0 未购买 1 已购买
     */
    @GetMapping("/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId);

    /**
     * 查询用户已购买的声音ID列表
     * @param albumId 专辑id
     * @return 在该专辑下已经购买的声音ID列表
     */
    @GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId);

    /**
     * 用户支付成功后，虚拟物品发货 内部接口：订单服务调用, 不能加 login 注解，因为还有一种支付方式：微信，当支付成功后，微信会回调我们的服务器，不携带令牌
     * @param vo 虚拟物品信息
     * @return 虚拟物品发货结果
     */
    @PostMapping("/userInfo/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo vo);

    /**
     * 每天凌晨12.00重置会员
     * @return
     */
    @GetMapping("/userinfo/resetVip")
    public Result resetVip();
}
