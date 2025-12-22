package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.BusinessException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderDerateService;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private OrderDerateService orderDerateService;


    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private AccountFeignClient accountFeignClient;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

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
            // derateAmount = BigDecimal.ZERO;

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

        // 5 todo 其他杂项数据的封装
        // 5.1 购买项目类型
        vo.setItemType(itemType);

        // 5.2 流水号：防止订单的重复提交 前缀:userId value-> 流水号值（uuid） ttl = 5min
        /**
         * 1. 网络卡顿，用户重复提交订单
         * 2. 成功提交订单，回退到订单确认页面，用户重复提交订单
         */
        // 5.2.1 生成流水号
        String key = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(key, tradeNo, 5, TimeUnit.MINUTES);
        vo.setTradeNo(tradeNo);

        // 5.3 时间戳
        vo.setTimestamp(System.currentTimeMillis());

        // 5.4 签名：防止数据被篡改
        /**
         * 如果没有签名，当数据被修改，就会导致业务异常
         */
        // 5.4.1 生成签名：支付方式不能放进去，因为提交订单的时候是不知道用什么支付方式的，但是下单的时候，会选择支付方式，这就会导致签名一定不一致
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(vo, false, true);
        String sign = SignHelper.getSign(stringObjectMap);
        vo.setSign(sign);
        return vo;
    }

    /**
     * 提交订单
     * @param vo 订单信息
     * @param userId 当前用户ID
     * @return
     */
    @Override
    public Map<String, String> submitOrder(OrderInfoVo vo, Long userId) {
        // 1. 验证订单是否重复提交，采用 lua 脚本，保证判断和删除的原子性
        // 1.1 拿到当前订单的流水号
        String tradeNo = vo.getTradeNo();
        String key = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;

        // 1.2 判断是否与 redis 中保存的流水号一致，如果一致，则说明重复提交了，不一致，就删除掉
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>(luaScript, Boolean.class);

        Boolean result = redisTemplate.execute(script, Collections.singletonList(key), tradeNo);
        if(!result){
            // 判重失败，说明之前已经提交了订单，直接结束业务
            throw new BusinessException(500, "验证订单流水号失败");
        }


        // 2. 验证签名
        // 2.1 将 vo --> map 由于在获取签名的时候，支付方式没加入到签名中，所以需要将这个值从 map 中移除
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(vo);
        stringObjectMap.remove("payWay");
        // 2.2 验证签名
        SignHelper.checkSign(stringObjectMap);


        // 核心业务 ================================================================================
        // 3. 保存订单信息
        OrderInfo orderInfo = this.saveOrderInfo(vo, userId); // 返回保存后的订单
        // 4. 判断支付方式
        // 余额支付
        String payWay = vo.getPayWay();
        if(SystemConstant.ORDER_PAY_ACCOUNT.equals(payWay)){
            // 4.1 先去封装 accountDeductVo
            AccountDeductVo accountDeductVo = new AccountDeductVo();
            accountDeductVo.setOrderNo(orderInfo.getOrderNo());
            accountDeductVo.setUserId(userId);
            accountDeductVo.setAmount(orderInfo.getOrderAmount());
            accountDeductVo.setContent(orderInfo.getOrderTitle());

            // 4.2 尝试去扣减用户余额，如果成功，就会修改用户的账户信息以及操作日志
            Result resp1 = accountFeignClient.CheckAndDeduct(accountDeductVo);// 扣款远程接口
            if(resp1.getCode().equals(ResultCodeEnum.SUCCESS.getCode())){
                // 4.3 扣减成功，修改订单状态
                orderInfo.setOrderStatus("0902");
                // 4.4 更新订单
                this.updateById(orderInfo);

                // 5. 发放权益（会员|专辑|声音） 调用用户微服务提供的接口
                UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
                userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
                userPaidRecordVo.setUserId(userId);
                userPaidRecordVo.setItemType(orderInfo.getItemType());
                userPaidRecordVo.setItemIdList((orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId)).toList());
                Result resp2 = userFeignClient.savePaidRecord(userPaidRecordVo);// 发放商品权益远程接口
                if(resp2.getCode().equals(ResultCodeEnum.SUCCESS.getCode())){
                    log.info("liutianba7：发放商品成功");
                }else{
                    throw new BusinessException(resp2.getCode(), resp2.getMessage());
                }
            }else{
                throw new BusinessException(resp1.getCode(), resp1.getMessage());
            }


        }
        // 微信支付
        else if(SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay))
        {

        }

        // 6. 延迟队列，将超时的订单关闭

        // 7. 响应结果
        return Map.of("orderNo", orderInfo.getOrderNo());
    }

    @Override
    public OrderInfo saveOrderInfo(OrderInfoVo vo, Long userId) {
        // 1. 保存订单数据
        OrderInfo orderInfo = BeanUtil.copyProperties(vo, OrderInfo.class);
        orderInfo.setUserId(userId);
        orderInfo.setOrderStatus("0901"); // @Schema(description = "订单状态：0901-未支付 0902-已支付 0903-已取消")
        List<OrderDetailVo> orderDetailVoList = vo.getOrderDetailVoList();
        if(!CollectionUtils.isEmpty(orderDetailVoList)){
            orderInfo.setOrderTitle(orderDetailVoList.get(0).getItemName());
        }
        // 唯一索引，且趋势递增：当天日期 + 雪花算法
        String orderNo = DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        orderInfo.setOrderNo(orderNo);
        // 保存当前订单信息
        orderInfoMapper.insert(orderInfo);
        // 获取当前订单id
        Long orderId = orderInfo.getId();

        // 2. 保存订单明细数据
        if(!CollectionUtils.isEmpty(orderDetailVoList)){
            List<OrderDetail> orderDetailList = orderDetailVoList.stream().map(orderDetailVo -> {
                OrderDetail orderDetail =BeanUtil.copyProperties(orderDetailVo, OrderDetail.class);
                orderDetail.setOrderId(orderId);
                return orderDetail;
            }).toList();
            orderDetailService.saveBatch(orderDetailList);
            orderInfo.setOrderDetailList(orderDetailList);
        }

        // 3. 保存订单减免数据
        List<OrderDerateVo> orderDerateVoList = vo.getOrderDerateVoList();
        if(!CollectionUtils.isEmpty(orderDerateVoList)){
            List<OrderDerate> orderDerateList = orderDerateVoList.stream().map(orderDerateVo -> {
                OrderDerate orderDerate =BeanUtil.copyProperties(orderDerateVo, OrderDerate.class);
                orderDerate.setOrderId(orderId);
                return orderDerate;
            }).toList();
            orderDerateService.saveBatch(orderDerateList);
            orderInfo.setOrderDerateList(orderDerateList);
        }
        return orderInfo;
    }

    @Override
    @Transactional
    public OrderInfo getOrderInfo(String orderId) {
        // 1. 查询订单信息
        LambdaQueryWrapper<OrderInfo> wr1 = new LambdaQueryWrapper<>();
        wr1.eq(OrderInfo::getOrderNo, orderId);
        OrderInfo orderInfo = orderInfoMapper.selectOne(wr1);
        Assert.notNull(orderInfo, "订单不存在");
        if(orderInfo.getPayWay().equals(SystemConstant.ORDER_PAY_WAY_WEIXIN)){
            orderInfo.setPayWayName("微信支付");
        }else if(orderInfo.getPayWay().equals(SystemConstant.ORDER_PAY_ACCOUNT)){
            orderInfo.setPayWayName("余额支付");
        }
        // 2. 查询订单详情信息
        LambdaQueryWrapper<OrderDetail> wr2 = new LambdaQueryWrapper<>();
        wr2.eq(OrderDetail::getOrderId, orderInfo.getId());
        List<OrderDetail> list = orderDetailService.list(wr2);
        orderInfo.setOrderDetailList(list);
        // 3. 查询订单减免信息
        LambdaQueryWrapper<OrderDerate> wr3 = new LambdaQueryWrapper<>();
        wr3.eq(OrderDerate::getOrderId, orderInfo.getId());
        List<OrderDerate> derateList = orderDerateService.list(wr3);
        if(!CollectionUtils.isEmpty(derateList)){
            orderInfo.setOrderDerateList(derateList);
        }

        return orderInfo;
    }
}
