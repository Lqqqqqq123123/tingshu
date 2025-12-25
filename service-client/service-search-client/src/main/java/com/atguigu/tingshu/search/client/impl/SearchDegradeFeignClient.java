package com.atguigu.tingshu.search.client.impl;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.client.SearchFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: atguigu
 * @create: 2023-12-05 22:23
 */

@Slf4j
@Component
public class SearchDegradeFeignClient implements SearchFeignClient {
    @Override
    public Result updateAlbumRanking(Integer top) {
        log.info("[远程调用专辑服务]的 [updateAlbumRanking] 接口出现异常，降级处理");
        return null;
    }
}
