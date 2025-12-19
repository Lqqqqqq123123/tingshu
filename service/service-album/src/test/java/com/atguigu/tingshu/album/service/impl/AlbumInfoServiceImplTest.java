package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AlbumInfoServiceImplTest {

    @Autowired
    private AlbumInfoService albumInfoService;

    @Test
    void batchUpperToBloom() {
        albumInfoService.batchUpperToBloom();
    }
}