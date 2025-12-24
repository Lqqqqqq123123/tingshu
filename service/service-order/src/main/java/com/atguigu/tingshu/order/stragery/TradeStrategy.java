package com.atguigu.tingshu.order.stragery;

import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;

/**
 * trade 方法的抽象策略接口，对不同的商品，有不同的 orderInfoVo 的处理逻辑
 * @author liutianba7
 * @create 2025/12/24 09:47
 */
public interface TradeStrategy {
    public OrderInfoVo trade(TradeVo tradeVo, Long userId);
}
