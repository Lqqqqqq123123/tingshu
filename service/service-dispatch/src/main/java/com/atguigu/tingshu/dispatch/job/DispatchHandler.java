package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchHandler {

    @Autowired
    private SearchFeignClient searchFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 定时更新专辑排行榜数据
     */
    @XxlJob("updateAlbumRanking")
    public void updateAlbumRanking() {
        log.info("Xxj-Job：更新专辑排行");
        searchFeignClient.updateAlbumRanking(10);
    }

    /**
     * 充值已经到期的会员的标识
     */
    @XxlJob("resetVip")
    public void resetVip() {
        log.info("Xxj-Job：重置会员标识");
        userFeignClient.resetVip();
    }
}