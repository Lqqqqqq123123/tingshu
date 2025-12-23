package com.atguigu.tingshu.user.strategy;

import com.atguigu.tingshu.vo.user.UserPaidRecordVo;

/**
 * 对不同商品权益发放的抽象策略类
 * @author liutianba7
 * @create 2025/12/23 09:40
 */
public interface DeliveryStrategy {
    public void delivery(UserPaidRecordVo vo);
}
