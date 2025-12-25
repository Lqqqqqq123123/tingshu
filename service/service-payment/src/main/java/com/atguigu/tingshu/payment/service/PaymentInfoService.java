package com.atguigu.tingshu.payment.service;

import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction;

public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 根据支付类型与订单号保存支付信息
     * @param paymentType 支付类型
     * @param orderNo 订单号
     * @return 支付信息
     */
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo);

    /**
     * 更新本地交易记录
     * @param transaction 微信交易记录
     */
    void updatePaymentInfo(Transaction transaction);
}
