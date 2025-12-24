package com.atguigu.tingshu.order.stragery.factory;

import com.atguigu.tingshu.order.stragery.TradeStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 交易策略工厂类
 * @author liutianba7
 * @create 2025/12/24 09:59
 */
@Slf4j
@Component
public class TradeStrategyFactory {

    @Autowired
    public Map<String, TradeStrategy> tradeStrategyMap;

    /**
     * 根据itemType获取交易策略
     * @param itemType 商品类型
     * @return 交易策略
     */
    public TradeStrategy getTradeStrategy(String itemType){
        if(tradeStrategyMap.containsKey(itemType)){
            return tradeStrategyMap.get(itemType);
        }
        log.info("未找到对应的交易策略");
        throw new RuntimeException("未找到对应的交易策略");
    }
}
