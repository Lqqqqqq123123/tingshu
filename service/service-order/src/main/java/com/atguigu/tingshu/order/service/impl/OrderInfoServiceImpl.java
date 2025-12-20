package com.atguigu.tingshu.order.service.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumFeignClient albumFeignClient;

    /**
     * 三种商品（VIP会员、专辑、声音）订单结算,渲染订单结算页面
     *
     * @param userId  当前用户ID
     * @param tradeVo (购买项目类型、购买项目ID、声音数量)
     * @return 订单VO信息
     */
    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
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


        // 2.3 初始化杂项信息


        // 2.4 获取本次交易类型
        String itemType = tradeVo.getItemType();

        // 3. 根据不同商品类型去完成订单数据的渲染
        // 3.1 vip 允许重复购买
        if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)) {

            // 1. 根据商品id去获得vip服务配置信息
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfigById(tradeVo.getItemId()).getData();
            Assert.notNull(vipServiceConfig, "vip服务配置不存在");

            // 2. 封装数据

            // 2.1 价格信息
            originalAmount = originalAmount.add(vipServiceConfig.getPrice());
            orderAmount = orderAmount.add(vipServiceConfig.getDiscountPrice());


            // 2.2 订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(vipServiceConfig.getId());
            orderDetailVo.setItemName("VIP：" + vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVoList.add(orderDetailVo);

            // 2.3 订单减免信息
            // 存在减免
            if (originalAmount.compareTo(orderAmount) == 1) {
                derateAmount = originalAmount.subtract(orderAmount);
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("开通vip服务优惠：" + derateAmount);
                orderDerateVoList.add(orderDerateVo);
            }

        }
        // 3.2 专辑:不允许重复购买
        else if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
            // 1. 先去判断之前购买过当前专辑没
            Boolean isPaidAlbum = userFeignClient.isPaidAlbum(tradeVo.getItemId()).getData();

            // 2. 如果购买过，则直接结束业务
            Assert.isTrue(!isPaidAlbum, "当前用户已购买过此专辑" + tradeVo.getItemId());

            // 3. 否则正常封装当前业务的数据
            // 3.1 去拿到当前用户的数据
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
            Assert.notNull(userInfoVo, "用户信息不存在");
            // 3.2 拿到专辑的数据
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(tradeVo.getItemId()).getData();
            Assert.notNull(albumInfo, "专辑信息不存在");

            // 3.3 计算价格
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


            // 3.4 封装数据
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

        }
        // 3.3 声音
        else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)) {

            // 1. 先去获得当前要购买的声音数量以及起始声音id
            Long id = tradeVo.getItemId();
            Integer trackCount = tradeVo.getTrackCount();


            // 2. 远程调用 findWaitBuyTrackInfoList 获取要购买的声音信息
            List<TrackInfo> trackinfoList = albumFeignClient.findWaitBuyTrackInfoList(id, trackCount).getData();
            Assert.notNull(trackinfoList, "声音信息不存在");

            // 3. 价格计算(单集购买没有折扣）
            // 3.1 获取专辑信息，从而拿到声音的单价信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(trackinfoList.get(0).getAlbumId()).getData();
            Assert.notNull(albumInfo, "专辑信息不存在");
            BigDecimal price = albumInfo.getPrice();

            // 3.2 计算价格 总价 = 单价 * 数量
            int count = trackinfoList.size();
            originalAmount = price.multiply(new BigDecimal(count));
            orderAmount = originalAmount;
            derateAmount = BigDecimal.ZERO;

            // 3.3 封装数据
            for (TrackInfo trackInfo : trackinfoList) {
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemId(trackInfo.getId());
                orderDetailVo.setItemName("声音：" + trackInfo.getTrackTitle());
                orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                orderDetailVo.setItemPrice(price);
                orderDetailVoList.add(orderDetailVo);
            }

        }

        // 4. 封装数据并返回
        // 4.1 订单金额信息
        vo.setOrderAmount(orderAmount);
        vo.setOriginalAmount(originalAmount);
        vo.setDerateAmount(derateAmount);
        // 4.2 订单明细
        vo.setOrderDetailVoList(orderDetailVoList);
        vo.setOrderDerateVoList(orderDerateVoList);

        // todo 其他杂项数据的封装

        return vo;


    }
}
