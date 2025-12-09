package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.simpleframework.xml.Path;
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

    @Operation(summary = "根据id删除专辑")
    @DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
    public Result removeAlbumInfo(@PathVariable Long id) {
        albumInfoService.removeAlbumInfo(id);
        return Result.ok();
    }


    @Operation(summary = "根据id查询专辑")
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(id);
        return Result.ok(albumInfo);
    }

    @Operation(summary = "更新专辑")
    @PutMapping("/albumInfo/updateAlbumInfo/{id}")
    public Result updateAlbumInfo(@PathVariable("id") Long id, @Validated @RequestBody AlbumInfoVo vo){
        albumInfoService.updateAlbumInfo(id, vo);
        return Result.ok();
    }


}

