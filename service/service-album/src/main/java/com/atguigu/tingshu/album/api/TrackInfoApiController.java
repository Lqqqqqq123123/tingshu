package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.cache.RedisCache;
import com.atguigu.tingshu.common.login.Login;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

	@Autowired
	private TrackInfoService trackInfoService;
    @Autowired
    private VodService vodService;

    @Login
    @Operation(summary = "上传声音到云点播平台，这里是tencent")
    @PostMapping("/trackInfo/uploadTrack")
    public Result<Map<String, String>> uploadTrack(@RequestPart("file") MultipartFile file) {
        Map<String, String> map = vodService.uploadTrack(file);
        return Result.ok(map);
    }


    /**
     * 保存声音
     * @param trackInfoVo
     * @return
     */
    @Login
    @Operation(summary = "保存声音")
    @PostMapping("/trackInfo/saveTrackInfo")
    public Result saveTrackInfo(@Validated @RequestBody TrackInfoVo trackInfoVo) {
        //1.获取用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务层保存声音
        trackInfoService.saveTrackInfo(trackInfoVo, userId);
        return Result.ok();
    }


    /**
     * 条件分页查询当前用户声音列表（包含声音统计信息）
     *
     * @param page           页码
     * @param limit          页大小
     * @param trackInfoQuery 查询条件
     * @return
     */
    @Login
    @Operation(summary = "条件分页查询当前用户声音列表（包含声音统计信息）")
    @PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
    public Result<Page<TrackListVo>> getUserTrackPage(
            @PathVariable int page,
            @PathVariable int limit,
            @RequestBody TrackInfoQuery trackInfoQuery
    ) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        trackInfoQuery.setUserId(userId);
        //2.构建分页所需分页对象
        Page<TrackListVo> pageInfo = new Page<>(page, limit);
        //3.查询业务层(持久层)获取分页数据
        pageInfo = trackInfoService.getUserTrackPage(pageInfo, trackInfoQuery);
        return Result.ok(pageInfo);
    }

    /**
     * 根据声音ID查询声音信息：即使内部接口，也是外部接口，供用户微服务使用
     *
     * @param id
     * @return
     */
    @Login
    @RedisCache(prefix = "album:trackInfo:", timeout = 3600 , timeunit = TimeUnit.SECONDS)
    @Operation(summary = "根据声音ID查询声音信息")
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    // 这里直接掉的 mp 的服务层方法，所以缓存注解只能加到改控制器上
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id) {

        TrackInfo trackInfo = trackInfoService.getById(id);
        return Result.ok(trackInfo);
    }

    /**
     * 修改声音信息
     * @param id
     * @param trackInfo
     * @return
     */
    @Login
    @Operation(summary = "修改声音信息")
    @PutMapping("/trackInfo/updateTrackInfo/{id}")
    public Result updateTrackInfo(@PathVariable Long id, @RequestBody TrackInfo trackInfo){
        trackInfoService.updateTrackInfo(trackInfo);
        return Result.ok();
    }

    /**
     * 删除声音记录
     * @param id
     * @return
     */
    @Login
    @Operation(summary = "删除声音记录")
    @DeleteMapping("/trackInfo/removeTrackInfo/{id}")
    public Result removeTrackInfo(@PathVariable Long id){
        trackInfoService.removeTrackInfo(id);
        return Result.ok();
    }


    /**
     * 需求：用户未登录，可以给用户展示声音列表；用户已登录，可以给用户展示声音列表，并动态渲染付费标识
     * 分页查询专辑下声音列表（动态渲染付费标识）
     * @param albumId
     * @param page
     * @param limit
     * @return
     */
    @Login(required = false)
    @Operation(summary = "分页查询专辑下声音列表（动态渲染付费标识）")
    @GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
    public Result<IPage<AlbumTrackListVo>> findAlbumTrackPage(
            @PathVariable Long albumId,
            @PathVariable Long page,
            @PathVariable Long limit) {
        //1.尝试获取用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.构建分页对象：封装当前页码、每页记录数
        IPage<AlbumTrackListVo> pageInfo = new Page<>(page, limit);

        //3.调用业务逻辑层->持久层：封装：总记录数，总页数，当前页数据
        pageInfo = trackInfoService.findAlbumTrackPage(pageInfo, albumId, userId);

        //4.响应分页对象
        return Result.ok(pageInfo);
    }

    /**
     * 查询声音统计信息
     * @param trackId
     * @return TrackStatVo
     */
    @Operation(summary = "查询声音统计信息")
    @GetMapping("/trackInfo/getTrackStatVo/{trackId}")
    public Result<TrackStatVo> getTrackStatVo(@PathVariable Long trackId){
        TrackStatVo trackStatVo = trackInfoService.getTrackStatVo(trackId);
        return Result.ok(trackStatVo);
    }

    /**
     * 基于用户选择的声音，动态的获取为购买的声音数量，得到声音购买列表
     * @param trackId
     * @return List<Map<String, Object>>
     */
    @Login
    @Operation(summary = "基于用户选择的声音，动态的获取为购买的声音数量，得到声音购买列表")
    @GetMapping("/trackInfo/findUserTrackPaidList/{trackId}")
    public Result<List<Map<String, Object>>> findUserTrackPaidList(@PathVariable Long trackId) {
        Long userId = AuthContextHolder.getUserId();
        List<Map<String, Object>> list = trackInfoService.findUserTrackPaidList(trackId, userId);
        return Result.ok(list);
    }

    /**
     * 查询用户未购买声音列表：内部接口，供订单服务调用
     * @param trackId 声音ID
     * @param trackCount 当前用户要购买的声音数量
     * @return List<TrackInfo> 从trackid的序号开始，获取trackCount个用户未购买的声音
     */
    @Login
    @Operation(summary = "查询用户未购买声音列表")
    @GetMapping("/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> findWaitBuyTrackInfoList(@PathVariable Long trackId, @PathVariable Integer trackCount) {
        Long userId = AuthContextHolder.getUserId();
        List<TrackInfo> list = trackInfoService.findWaitBuyTrackInfoList(userId, trackId, trackCount);
        return Result.ok(list);
    }


}

