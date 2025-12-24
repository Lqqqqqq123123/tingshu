package com.atguigu.tingshu.order.stragery.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.order.stragery.TradeStrategyAbstract;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author liutianba7
 * @create 2025/12/24 09:56
 */
@Slf4j
@Component(value = SystemConstant.ORDER_ITEM_TYPE_ALBUM)
public class AlbumTradeStrategy extends TradeStrategyAbstract {

    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        log.info("liutianba7:专辑商品订单结算");

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

        // 2.3 获取本次交易类型
        String itemType = tradeVo.getItemType();

        // 3. 先去判断之前购买过当前专辑没,如果购买过，则直接结束业务
        Boolean isPaidAlbum = userFeignClient.isPaidAlbum(tradeVo.getItemId()).getData();

        Assert.isTrue(!isPaidAlbum, "当前用户已购买过此专辑" + tradeVo.getItemId());

        // 4. 否则正常封装当前业务的数据
        // 4.1 去拿到当前用户的数据
        UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
        Assert.notNull(userInfoVo, "用户信息不存在");
        // 4.2 拿到专辑的数据
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(tradeVo.getItemId()).getData();
        Assert.notNull(albumInfo, "专辑信息不存在");

        // 4.3 计算价格
        BigDecimal price = albumInfo.getPrice();
        BigDecimal discount = albumInfo.getDiscount(); // 不打折是 -1
        BigDecimal vipDiscount = albumInfo.getVipDiscount(); // 不打折是 -1
        if (discount.compareTo(new BigDecimal("-1")) == 0) {
            discount = BigDecimal.TEN;
        }
        if (vipDiscount.compareTo(new BigDecimal("-1")) == 0) {
            vipDiscount = BigDecimal.TEN;
        }

        int isVip = 0; // 0:普通用户  1:VIP会员"
        if (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().after(new Date())) {
            isVip = 1;
        }
        originalAmount = price;
        BigDecimal lastDiscount = isVip == 1 ? vipDiscount : discount;
        orderAmount = originalAmount.multiply(lastDiscount).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP); // 这里折扣是从0.1-9.9,所以需要除个10


        // 4.4 订单详情与订单减免信息
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemId(albumInfo.getId());
        orderDetailVo.setItemName("专辑：" + albumInfo.getAlbumTitle());
        orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
        orderDetailVo.setItemPrice(originalAmount);
        orderDetailVoList.add(orderDetailVo);

        if (originalAmount.compareTo(orderAmount) == 1) {
            derateAmount = originalAmount.subtract(orderAmount);
            OrderDerateVo orderDerateVo = new OrderDerateVo();
            orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
            orderDerateVo.setDerateAmount(derateAmount);
            orderDerateVo.setRemarks("购买专辑优惠：" + derateAmount);
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
