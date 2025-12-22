package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.extension.service.IService;
import me.chanjar.weixin.common.error.WxErrorException;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 根据 code 去对接微信服务端进行登录，活得 openid
     * @param code
     * @return
     */
    Map<String, String> wxLogin(String code) throws WxErrorException;

    /**
     * 获取当前登录用户基本信息
     *
     * @return Vo
     */
    default UserInfoVo getUserInfo(Long userId) {
        return null;
    }

    /**
     * 更新当前用户信息
     * @param userInfoVo
     * @return
     */
    void updateUser(UserInfoVo userInfoVo);

    /**
     * 提交需要检查购买状态声音ID列表，响应每个声音购买状态
     * @param userId
     * @param albumId
     * @param ids 待检查购买状态声音ID列表
     * @return
     */
    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> ids);

    /**
     * 判断当前用户是否购买过指定专辑
     * @param albumId
     * @return 0 未购买 1 已购买
     */
    Boolean isPaidAlbum(Long userId, Long albumId);

    /**
     * 查询用户已购买的声音ID列表
     * @param albumId 专辑id
     * @param userId 用户id
     * @return 在该专辑下已经购买的声音ID列表
     */
    List<Long> findUserPaidTrackList(Long userId, Long albumId);

    /**
     * 用户支付成功后，虚拟物品发货 内部接口：订单服务调用
     * @param vo 虚拟物品信息
     * @return 虚拟物品发货结果
     */
    void savePaidRecord(UserPaidRecordVo vo);
}
