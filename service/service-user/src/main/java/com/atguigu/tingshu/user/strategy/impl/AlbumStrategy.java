package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.service.UserPaidAlbumService;
import com.atguigu.tingshu.user.strategy.DeliveryStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 专辑权益发放策略
 * @author liutianba7
 * @create 2025/12/23 09:48
 */
@Slf4j
@Component("1001")
public class AlbumStrategy implements DeliveryStrategy {

    @Autowired
    private UserPaidAlbumService userPaidAlbumService;
    @Override
    public void delivery(UserPaidRecordVo vo) {
        log.info("liutianba7:专辑发放策略");
        // 1.1.1 看看是否已经有了专辑的支付记录
        LambdaQueryWrapper<UserPaidAlbum> wr = new LambdaQueryWrapper<>();
        wr.eq(UserPaidAlbum::getOrderNo, vo.getOrderNo()).select(UserPaidAlbum::getId); // 索引覆盖，避免回表查询
        Long count = userPaidAlbumService.count(wr);
        if(count > 0){
            return;
        }
        // 1.1.2 保存专辑支付记录
        UserPaidAlbum paidAlbum = new UserPaidAlbum();
        paidAlbum.setOrderNo(vo.getOrderNo());
        paidAlbum.setAlbumId(vo.getItemIdList().get(0));
        paidAlbum.setUserId(vo.getUserId());
        userPaidAlbumService.save(paidAlbum);
    }
}


