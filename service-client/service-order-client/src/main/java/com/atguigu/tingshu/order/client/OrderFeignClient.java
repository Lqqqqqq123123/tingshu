package com.atguigu.tingshu.order.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 订单模块远程调用API接口
 * </p>
 *
 * @author liutianba7
 */
@FeignClient(value = "service-order", fallback = OrderDegradeFeignClient.class, path = "/api/order")
public interface OrderFeignClient {

    /**
     * 根据订单id查询订单信息
     * @param orderId 订单id
     * @return 订单信息
     */
    @GetMapping("/orderInfo/getOrderInfo/{orderId}")
    public Result<OrderInfo> getOrderInfo(@PathVariable("orderId") String orderId);

}
