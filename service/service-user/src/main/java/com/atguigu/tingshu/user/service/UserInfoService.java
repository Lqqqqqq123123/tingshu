package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;
import me.chanjar.weixin.common.error.WxErrorException;

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
}
