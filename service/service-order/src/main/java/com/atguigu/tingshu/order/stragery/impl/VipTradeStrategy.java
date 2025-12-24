package com.atguigu.tingshu.order.stragery.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.stragery.TradeStrategyAbstract;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liutianba7
 * @create 2025/12/24 09:56
 */
@Slf4j
@Component(value = SystemConstant.ORDER_ITEM_TYPE_VIP)
public class VipTradeStrategy extends TradeStrategyAbstract {

    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        log.info("liutianba7:会员商品订单结算");

        // 1. 创建 vo
        OrderInfoVo vo = new OrderInfoVo();

        // 2. 初始化
        // 2.1 价格相关数据
        BigDecimal originalAmount = new BigDecimal("0.00");
        BigDecimal derateAmount = new BigDecimal("0.00");
        BigDecimal orderAmount = new BigDecimal("0.00");

        // 2.2 订单明细列表
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();


        // 3. 根据不同商品类型去完成订单数据的渲染 vip 允许重复购买
        // 3.1 根据商品id去获得vip服务配置信息
        VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfigById(tradeVo.getItemId()).getData();
        Assert.notNull(vipServiceConfig, "vip服务配置不存在");

        // 4. 封装数据

        // 4.1 价格信息
        originalAmount = originalAmount.add(vipServiceConfig.getPrice());
        orderAmount = orderAmount.add(vipServiceConfig.getDiscountPrice());


        // 4.2 订单明细
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemId(vipServiceConfig.getId());
        orderDetailVo.setItemName("VIP：" + vipServiceConfig.getName());
        orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
        orderDetailVo.setItemPrice(originalAmount);
        orderDetailVoList.add(orderDetailVo);

        // 4.3 订单减免信息
        // 存在减免
        if (originalAmount.compareTo(orderAmount) == 1) {
            derateAmount = originalAmount.subtract(orderAmount);
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("开通vip服务优惠：" + derateAmount);
            orderDerateVoList.add(orderDerateVo);
        }


        // 5. 封装数据并返回
        // 5.1 订单金额信息
        vo.setOrderAmount(orderAmount);
        vo.setOriginalAmount(originalAmount);
        vo.setDerateAmount(derateAmount);
        // 5.2 订单明细
        vo.setOrderDetailVoList(orderDetailVoList);
        vo.setOrderDerateVoList(orderDerateVoList);

        // 5.3 其他杂项数据的封装
        // 5.3.1 购买项目类型
        vo.setItemType(tradeVo.getItemType());

        // 5.3.2 流水号：防止订单的重复提交 前缀:userId value-> 流水号值（uuid） ttl = 5min
        /**
         * 1. 网络卡顿，用户重复提交订单
         * 2. 成功提交订单，回退到订单确认页面，用户重复提交订单
         */
        generate_tradeNo(vo, userId);

        // 5.3.3 时间戳
        vo.setTimestamp(System.currentTimeMillis());

        // 5.3.4 签名：防止数据被篡改
        /**
         * 如果没有签名，当数据被修改，就会导致业务异常
         */
        // 5.3.5 生成签名：支付方式不能放进去，因为提交订单的时候是不知道用什么支付方式的，但是下单的时候，会选择支付方式，这就会导致签名一定不一致
        generate_sign(vo);

        return vo;
    }
}
