package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.Login;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;

    /**
     * 根据用户ID查询用户/主播基本信息
     * @param userId
     * @return
     */
    @Operation(summary = "根据用户ID查询用户/主播基本信息")
    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId){
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }


    /**
     * 提交需要检查购买状态声音ID列表，响应每个声音购买状态
     * @param userId
     * @param albumId
     * @param ids 待检查购买状态声音ID列表
     * @return [声音id:购买状态] 0 未购买 1 已购买
     */
    @Operation(summary = "提交需要检查购买状态声音ID列表，响应每个声音购买状态")
    @PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(
            @PathVariable(value = "userId") Long userId,
            @PathVariable(value = "albumId") Long albumId,
            @RequestBody List<Long> ids
    ) {
        Map<Long, Integer> map = userInfoService.userIsPaidTrack(userId, albumId, ids);
        return Result.ok(map);
    }

    /**
     * 判断当前用户是否购买过指定专辑：内部接口，被订单服务调用
     * @param albumId
     * @return 0 未购买 1 已购买
     */
    @Login
    @Operation(summary = "判断当前用户是否购买过指定专辑")
    @GetMapping("/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId) {
        Long userId = AuthContextHolder.getUserId();
        Boolean isPaidAlbum = userInfoService.isPaidAlbum(userId, albumId);
        return Result.ok(isPaidAlbum);
    }


    /**
     * 查询用户已购买的声音ID列表
     * @param albumId 专辑id
     * @return 在该专辑下已经购买的声音ID列表
     */
    @Login
    @Operation(summary = "查询用户已购买的声音ID列表")
    @GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId) {
        Long userId = AuthContextHolder.getUserId();
        List<Long> list = userInfoService.findUserPaidTrackList(userId, albumId);
        return Result.ok(list);
    }

}

