package com.atguigu.tingshu.album.task;

import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liutianba7
 * @create 2025/12/10 20:17
 */

@Slf4j
@Service
public class ReviewTask {

    @Autowired
    private AuditService auditService;

    @Autowired
    private TrackInfoService trackInfoService;
    // 每隔半个小时执行一次：测试通过
    // @Scheduled(cron = "0 0/30 * * * ? ")
    // @Scheduled(cron = "0/5 * * * * ? ")
    public void checkReviewTask() {
        log.info("开始执行定时任务");
        // 1. 查询所有状态为审核中的声音;
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getStatus, SystemConstant.TRACK_STATUS_REVIEWING);
        wrapper.select(TrackInfo::getReviewTaskId, TrackInfo::getId);
        List<TrackInfo> list = trackInfoService.list(wrapper);


        // 2. 通过 getReviewTaskResult 去查询这些任务的结果，只要返回pass，就存到 ids 中，然后批量更新数据库

        if(CollectionUtils.isEmpty(list))return ;

        List<TrackInfo> updated = list.stream()
                .map(trackInfo -> {
                    String result = auditService.getReviewTaskResult(trackInfo.getReviewTaskId());
                    if("pass".equals( result))
                        trackInfo.setStatus(SystemConstant.TRACK_STATUS_PASS);
                    else if("block".equals(result))
                        trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);
                    else if("review".equals(result))
                        trackInfo.setStatus(SystemConstant.TRACK_STATUS_REVIEW);
                    return trackInfo;
                })
                .toList();


        if(!CollectionUtils.isEmpty(updated))
            trackInfoService.updateBatchById(updated);
    }


}
