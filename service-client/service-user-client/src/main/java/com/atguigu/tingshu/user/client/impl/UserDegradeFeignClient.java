package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserDegradeFeignClient implements UserFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfoVo(Long userId) {
        log.info("[远程调用 user 服务的 getUserInfoVo 出现异常]");
        return null;
    }

    @Override
    public Result<Map<Long, Integer>> userIsPaidTrack(Long userId, Long albumId, List<Long> ids) {
        log.info("[远程调用 user 服务的 userIsPaidTrack 出现异常]");
        return null;
    }

    @Override
    public Result<VipServiceConfig> getVipServiceConfigById(Long id) {
        log.info("[远程调用 user 服务的 getVipServiceConfigById 出现异常]");
        return null;
    }

    @Override
    public Result<Boolean> isPaidAlbum(Long albumId) {
        log.info("[远程调用 user 服务的 isPaidAlbum 出现异常]");
        return null;
    }

    @Override
    public Result<List<Long>> findUserPaidTrackList(Long albumId) {
        log.info("[远程调用 user 服务的 findUserPaidTrackList 出现异常]");
        return null;
    }
}
