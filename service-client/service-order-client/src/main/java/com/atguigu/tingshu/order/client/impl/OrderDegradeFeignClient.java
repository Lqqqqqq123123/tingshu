package com.atguigu.tingshu.order.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderDegradeFeignClient implements OrderFeignClient {


    @Override
    public Result<OrderInfo> getOrderInfo(String orderId) {
        log.info("远程调用订单微服务的 【getOrderInfo】 失败了");
        return null;
    }

    @Override
    public Result orderPaySuccess(String orderNo) {
        log.info("远程调用订单微服务的 【orderPaySuccess】 失败了");
        return null;
    }
}

