package com.atguigu.tingshu.order.stragery.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.order.stragery.TradeStrategyAbstract;
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
@Component(value = SystemConstant.ORDER_ITEM_TYPE_TRACK)
public class TrackTradeStrategy extends TradeStrategyAbstract {

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        log.info("liutianba7:声音商品订单结算");
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


        // 3. 执行声音的订单数据结算
        // 3.1 先去获得当前要购买的声音数量以及起始声音id
        Long id = tradeVo.getItemId();
        Integer trackCount = tradeVo.getTrackCount();


        // 3.2 远程调用 findWaitBuyTrackInfoList 获取要购买的声音信息
        List<TrackInfo> trackinfoList = albumFeignClient.findWaitBuyTrackInfoList(id, trackCount).getData();
        Assert.notNull(trackinfoList, "声音信息不存在");

        // 4 价格计算(单集购买没有折扣）
        // 4.1 获取专辑信息，从而拿到声音的单价信息
        AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(trackinfoList.get(0).getAlbumId()).getData();
        Assert.notNull(albumInfo, "专辑信息不存在");
        BigDecimal price = albumInfo.getPrice();

        // 4.2 计算价格 总价 = 单价 * 数量
        int count = trackinfoList.size();
        originalAmount = price.multiply(new BigDecimal(count));
        orderAmount = originalAmount;
        // derateAmount = BigDecimal.ZERO;

        // 4.3 订单详情，声音无优惠信息
        for (TrackInfo trackInfo : trackinfoList) {
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(trackInfo.getId());
            orderDetailVo.setItemName("声音：" + trackInfo.getTrackTitle());
            orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
            orderDetailVo.setItemPrice(price);
            orderDetailVoList.add(orderDetailVo);
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
