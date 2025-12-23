package com.atguigu.tingshu.user.strategy.factory;

import com.atguigu.tingshu.user.strategy.DeliveryStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author liutianba7
 * @create 2025/12/23 09:51
 */
@Slf4j
@Component
public class DeliveryStrategyFactory {

    // 自动的注入该抽象策略的所有实现
    @Autowired
    /**
     * 自动注入：将该接口的所有实现类自动注入到该map中，key为实现类名称，value为实现类对象
     */
    private Map<String, DeliveryStrategy> deliveryStrategyMap;

    public DeliveryStrategy getDeliveryStrategy(String itemType){
        log.info("liutianba7:itemType:{}", itemType);
        if(deliveryStrategyMap.containsKey(itemType)){
            return deliveryStrategyMap.get(itemType);
        }
        log.error("liutianba7:未找到对应的策略类");
        throw new RuntimeException("未找到对应的策略类");
    }

}
