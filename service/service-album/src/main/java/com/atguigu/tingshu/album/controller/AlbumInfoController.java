package com.atguigu.tingshu.album.controller;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/album")
public class AlbumInfoController{

    private final AlbumInfoService albumInfoService;

    @RequestMapping("/get")
    public Result test(){
        return Result.ok(albumInfoService.getById(1));
    }
}
