package com.atguigu.tingshu;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author liutianba7
 * @create 2025/12/13 09:55
 */
@SpringBootTest
public class AlbumInfoOpenFeignTest {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Test
    public void testGetAlbumInfo() {
        Result<AlbumInfo> result = albumFeignClient.getAlbumInfo(1L);
        System.out.println(result);
    }

    @Test
    public void testGetCategoryView() {
        Result<BaseCategoryView> result = albumFeignClient.getCategoryView(1011L);
        System.out.println(result);
    }
}
