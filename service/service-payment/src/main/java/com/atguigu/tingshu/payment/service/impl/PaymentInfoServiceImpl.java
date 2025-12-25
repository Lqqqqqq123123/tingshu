package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.BusinessException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {


    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private AccountFeignClient accountFeignClient;
    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 根据支付类型与订单号保存支付信息
     * @param paymentType 支付类型
     * @param orderNo 订单号
     * @return 支付信息
     */
    @Override
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo) {
        // 1. 先查，存在，则返回
        LambdaQueryWrapper<PaymentInfo> wr = new LambdaQueryWrapper<>();
        wr.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo po = paymentInfoMapper.selectOne(wr);

        if(po != null){
            return po;
        }
        // 2. 不存在，创建,复制
        po = new PaymentInfo();

        // 2.1 支付类型
        po.setPaymentType(paymentType);
        // 2.2 订单编号
        po.setOrderNo(orderNo);
        // 2.3 支付方式
        po.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN); // 1101 微信 1102 支付宝 微信小程序只支持微信支付
        // 2.4 支付状态
        po.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID); // 1401 未支付 1402 已支付
        // 3.如果是充值余额
        if(SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)){
            // 3.0 远程调用账户微服务，根据订单号获取充值信息
            RechargeInfo rechargeInfo = accountFeignClient.getRechargeInfo(orderNo).getData();
            Assert.notNull(rechargeInfo, "远程调用账户微服务，根据订单号获取充值信息失败");
            if(!rechargeInfo.getRechargeStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)){
                throw new RuntimeException("订单状态有误");
            }
            // 3.1 用户id
            po.setUserId(rechargeInfo.getUserId());
            // 3.2 金额
            po.setAmount(rechargeInfo.getRechargeAmount());
            // 3.3 支付内容
            po.setContent("充值余额" + rechargeInfo.getRechargeAmount());

        }
        // 4.如果是正常订单
        else if(SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)){
            // 4.0 远程调用订单微服务，根据订单号获取订单信息
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
            Assert.notNull(orderInfo, "远程调用订单微服务，根据订单号获取订单信息失败");
            if(!orderInfo.getOrderStatus().equals(SystemConstant.ORDER_STATUS_UNPAID)){
                throw new RuntimeException("订单状态有误");
            }
            // 4.1 用户id
            po.setUserId(orderInfo.getUserId());
            // 4.2 金额
            po.setAmount(orderInfo.getOrderAmount());
            // 4.3 支付内容
            po.setContent(orderInfo.getOrderTitle());
        }

        paymentInfoMapper.insert(po);
        return po;
    }

    @Override
    public void updatePaymentInfo(Transaction transaction) {
        // 1. 获取商侧订单号
        String orderNo = transaction.getOutTradeNo();
        // 2. 获取支付信息
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        Assert.notNull(paymentInfo, "支付信息不存在");

        // 3. 判断支付状态 1401-未支付 1402-已支付
        if(SystemConstant.PAYMENT_STATUS_PAID.equals(paymentInfo.getPaymentStatus())){
            throw new RuntimeException("订单已支付");
        }

        // 4. 更新支付信息
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_PAID);
        paymentInfo.setOutTradeNo(transaction.getTransactionId());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(transaction.toString());
        paymentInfoMapper.updateById(paymentInfo);

        String paymentType = paymentInfo.getPaymentType();
        // 5. 根据不同的支付类型，调用不同的业务（更新订单信息 | 充值信息）
        if(SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)){
            // 5.1 远程调用方法，实现更新订单状态 + 发放用户权益
            Result result = orderFeignClient.orderPaySuccess(orderNo);

            if(result.getCode().intValue() != 200){
                throw new BusinessException(result.getCode(), result.getMessage());
            }

        }else if(SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType))
        {
            // 5.2 远程调用方法，实现更新充值订单状态以及账户余额
            Result result = accountFeignClient.rechargePaySuccess(orderNo);
            if(result.getCode().intValue() != 200){
                throw new BusinessException(result.getCode(), result.getMessage());
            }
        }


    }
}
