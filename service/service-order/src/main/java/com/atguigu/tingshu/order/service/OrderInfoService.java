package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 三种商品（VIP会员、专辑、声音）订单结算,渲染订单结算页面
     * @param userId 当前用户ID
     * @param tradeVo (购买项目类型、购买项目ID、声音数量)
     * @return 订单VO信息
     */
    OrderInfoVo trade(TradeVo tradeVo, Long userId);

    /**
     * 提交订单
     * @param vo 订单信息
     * @param userId 当前用户ID
     * @return
     */
    Map<String, String> submitOrder(OrderInfoVo vo, Long userId);

    /**
     * 保存订单信息的方法：
     * @param vo 订单信息
     * @param userId 用户ID
     * @return 保存后的订单信息
     */
    OrderInfo saveOrderInfo(OrderInfoVo vo, Long userId);

    /**
     * 根据订单ID查询订单信息
     * @param orderId 订单ID
     * @return 订单信息
     */
    OrderInfo getOrderInfo(String orderId);


}
