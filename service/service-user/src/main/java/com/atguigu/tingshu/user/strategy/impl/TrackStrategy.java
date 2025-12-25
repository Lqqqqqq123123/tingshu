package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.user.strategy.DeliveryStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 声音购权益发放策略
 * @author liutianba7
 * @create 2025/12/23 09:48
 */
@Slf4j
@Component("1002")
public class TrackStrategy implements DeliveryStrategy {
    @Autowired
    private UserPaidTrackService userPaidTrackService;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Override
    public void delivery(UserPaidRecordVo vo) {
        log.info("liutianba7:声音发放策略");
        // 1. 看看是否已经有了声音的支付记录
        LambdaQueryWrapper<UserPaidTrack> wr = new LambdaQueryWrapper<>();
        wr.eq(UserPaidTrack::getOrderNo, vo.getOrderNo()).select(UserPaidTrack::getId); // 索引覆盖，避免回表查询
        Long count = userPaidTrackService.count(wr);
        if(count > 0){
            return;
        }
        // 2. 查询当前一条声音的信息，拿到专辑ID
        TrackInfo data = albumFeignClient.getTrackInfo(vo.getItemIdList().get(0)).getData();
        Assert.notNull(data, "声音不存在");

        // 3. 批量保存声音支付记录
        List<UserPaidTrack> list = vo.getItemIdList()
                .stream()
                .map(itemId -> {
                    UserPaidTrack paidTrack = new UserPaidTrack();
                    paidTrack.setOrderNo(vo.getOrderNo());
                    paidTrack.setTrackId(itemId);
                    paidTrack.setUserId(vo.getUserId());
                    paidTrack.setAlbumId(data.getAlbumId());
                    return paidTrack;
                }).toList();
        userPaidTrackService.saveBatch(list);
        // todo: 保存账户日志记录
    }
}
