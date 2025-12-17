package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {


    /**
     * 获取声音播放进度
     *
     * @param trackId 声音ID
     * @return Result<BigDecimal>:当前用户对于当前声音的播放进度
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);

    /**
     * 更新声音播放进度
     * @param userListenProcessVo 声音播放进度信息
     * @return
     */
    void updateUserListenProcess(Long userId, UserListenProcessVo userListenProcessVo);

    /**
     * 获取当前用户最近播放声音
     * @return {albumId:1,trackId:12}
     */
    Map<String, Long> getLatelyTrack(Long userId);
}
