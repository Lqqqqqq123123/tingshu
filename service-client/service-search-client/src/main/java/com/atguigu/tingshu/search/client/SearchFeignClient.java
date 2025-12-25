package com.atguigu.tingshu.search.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.client.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 搜索模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-search", fallback = SearchDegradeFeignClient.class, path = "/api/search")
public interface SearchFeignClient {

    /**
     * 更新专辑排名：供执行器定时调用该接口
     * @param top
     * @return
     */
    @GetMapping("/albumInfo/updateAlbumRanking/{top}")
    public Result updateAlbumRanking(@PathVariable("top") @RequestParam(defaultValue = "10") Integer top);
}
