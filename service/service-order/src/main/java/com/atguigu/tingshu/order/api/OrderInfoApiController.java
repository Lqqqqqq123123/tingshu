package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.Login;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;


    /**
     * 三种商品（VIP会员、专辑、声音）订单结算,渲染订单结算页面
     * @param tradeVo (购买项目类型、购买项目ID、声音数量)
     * @return 订单VO信息
     */
    @Login
    @Operation(summary = "三种商品（VIP会员、专辑、声音）订单结算")
    @PostMapping("/orderInfo/trade")
    public Result<OrderInfoVo> trade(@RequestBody TradeVo tradeVo) {
        // 1. 获取当前用户 id
        Long userId = AuthContextHolder.getUserId();

        // 2. 获取订单信息
        OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo, userId);

        // 3. 返回
        return Result.ok(orderInfoVo);
    }

    /**
     * 根据前端传来的订单数据，创建订单
     * @param vo
     * @return
     */
    @Login
    @Operation(summary = "提交订单")
    @PostMapping("orderInfo/submitOrder")
    public Result<Map<String, String>> submitOrder(@RequestBody OrderInfoVo vo) {
        Long userId = AuthContextHolder.getUserId();
        Map<String, String> resp = orderInfoService.submitOrder(vo, userId);
        return Result.ok(resp);
    }

    /**
     * 根据订单id查询订单信息
     * @param orderId 订单id
     * @return 订单信息
     */
    @Operation(summary = "根据订单id查询订单信息")
    @GetMapping("/orderInfo/getOrderInfo/{orderId}")
    public Result<OrderInfo> getOrderInfo(@PathVariable("orderId") String orderId){

        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderId);

        return Result.ok(orderInfo);
    }


}

