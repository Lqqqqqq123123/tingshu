package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.login.Login;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumVoWithIdAndTitle;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;

    @Login
    @Operation(summary = "保存专辑信息")
    @PostMapping("/albumInfo/saveAlbumInfo")
    public Result saveAlbumInfo(@Validated @RequestBody AlbumInfoVo vo) {

        // 1. 获取当前用户 id
        Long userId = AuthContextHolder.getUserId();
        // 2. 调用 service 保存
        albumInfoService.savaAlbumInfo(vo, userId);
        // 3. 返回结果
        return Result.ok();
    }

    @Login
    @Operation(summary = "分页查询当前用户专辑列表")
    @PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
    public Result<IPage<AlbumListVo>> findUserAlbumPage(
            @PathVariable("page") Long page,
            @PathVariable("limit") Long limit,
            @RequestBody AlbumInfoQuery query){

        // 1. 获取当前用户 id
        Long userId = AuthContextHolder.getUserId();
        query.setUserId(userId);

        // 2. 新建分页对象
        IPage<AlbumListVo> res = new Page<>(page, limit);

        albumInfoService.findUserAlbumPage(res, query);

        return Result.ok(res);
    }

    @Login
    @Operation(summary = "根据id删除专辑")
    @DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
    public Result removeAlbumInfo(@PathVariable Long id) {
        albumInfoService.removeAlbumInfo(id);
        return Result.ok();
    }


    // @Login
    @Operation(summary = "根据id查询专辑")
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(id);
        return Result.ok(albumInfo);
    }

    @Login
    @Operation(summary = "更新专辑")
    @PutMapping("/albumInfo/updateAlbumInfo/{id}")
    public Result updateAlbumInfo(@PathVariable("id") Long id, @Validated @RequestBody AlbumInfoVo vo){
        albumInfoService.updateAlbumInfo(id, vo);
        return Result.ok();
    }


    /**
     * 查询当前用户所有专辑
     * @return
     */
    @Login
    @Operation(summary = "查询当前用户所有专辑,只返回专辑id与专辑名称")
    @GetMapping("/albumInfo/findUserAllAlbumList")

    public Result<List<AlbumVoWithIdAndTitle>> findUserAllAlbumList(){

        // 1. 获取当前用户 id
        Long userId = AuthContextHolder.getUserId();

        List<AlbumVoWithIdAndTitle> list = albumInfoService.list(
                new LambdaQueryWrapper<AlbumInfo>()
                        .select(AlbumInfo::getId, AlbumInfo::getAlbumTitle)
                        .eq(AlbumInfo::getUserId, userId)
                        .eq(AlbumInfo::getStatus, SystemConstant.ALBUM_STATUS_PASS)
                        .orderByDesc(AlbumInfo::getId)
                        // todo：由于在测试阶段将专辑都分配给了1号用户，所以该接口的查寻接近全表扫描，暂时解决方案（limit），最后的优化应该是前端做分页
                        .last("limit 100")
        )
                .stream()
                .map(t -> {
                    AlbumVoWithIdAndTitle vo = new AlbumVoWithIdAndTitle();
                    vo.setId(t.getId());
                    vo.setAlbumTitle(t.getAlbumTitle());
                    return vo;
                }).toList();

        return Result.ok(list);
    }





}

