package com.atguigu.tingshu.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.BusinessException;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@SuppressWarnings("all")
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;
    @Autowired
    private RSAAutoCertificateConfig rsaAutoCertificateConfig;
    @Autowired
    private WxPayV3Config wxPayV3Config;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 下单或充值选择微信支付，返回小程序拉起微信支付所需参数
     * * @param paymentType 支付类型：1301下单  1302充值
     *
     * @param orderNo 订单/充值订单编号
     * @return 返回结果包含以下字段：
     * <ul>
     * <li>timeStamp: 时间戳，从 1970 年 1 月 1 日 00:00:00 至今的秒数</li>
     * <li>nonceStr: 随机字符串，不长于 32 位</li>
     * <li>package: 订单详情扩展字符串，统一下单接口返回的 prepay_id 参数值，提交格式如：prepay_id=***</li>
     * <li>signType: 签名类型，默认为 RSA</li>
     * <li>paySign: 签名，使用 APIv3 (商户私钥)私钥对参数计算得出的签名值</li>
     * </ul>
     */
    @Override
    public Map<String, String> createJsapi(String paymentType, String orderNo) {

        try {

            // 1. 保存本地交易记录
            // 1.1 根据支付类型与订单号保存支付信息
            PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo);

            // 1.2 判断状态
            if (paymentInfo.getPaymentStatus().equals(SystemConstant.PAYMENT_STATUS_PAID)) {
                throw new RuntimeException("该订单已支付");
            }


            // 2. 对接微信支付
            //2.1 创建调用微信支付JSAPI业务对象
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();

            //2.2 创建预下单参数请求对象
            PrepayRequest request = new PrepayRequest();
            Amount amount = new Amount();
            //测试付款金额固定硬编码为1分
            amount.setTotal(1);
            request.setAmount(amount);
            request.setAppid(wxPayV3Config.getAppid());
            request.setMchid(wxPayV3Config.getMerchantId());
            request.setDescription(paymentInfo.getContent());
            request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
            //商户订单编号
            request.setOutTradeNo(orderNo);
            //小程序目前还未上线，仅支持应用下的开发者用户进行付款 故这里需要设置开发者用户的openid
            Payer payer = new Payer();
            payer.setOpenid("odo3j4qp-wC3HVq9Z_D9C0cOr0Zs");
            request.setPayer(payer);
            //2.3 调用微信获取response包含了调起支付所需的所有参数，可直接用于前端调起支付
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            if (response != null) {
                String timeStamp = response.getTimeStamp();
                String nonceStr = response.getNonceStr();
                String packageVal = response.getPackageVal();
                String signType = response.getSignType();
                String paySign = response.getPaySign();
                Map<String, String> map = new HashMap<>();
                map.put("timeStamp", timeStamp);
                map.put("nonceStr", nonceStr);
                map.put("package", packageVal);
                map.put("signType", signType);
                map.put("paySign", paySign);
                return map;
            }
            return null;
        } catch (Exception e) {
            log.error("微信下单失败");
            throw new BusinessException(500, e.getMessage());
        }
    }

    @Override
    public boolean queryPayStatus(String orderNo) {
//        // 1. 创建调用微信支付JSAPI业务对象
//        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
//        // 2. 创建请求对象
//        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
//        request.setMchid(wxPayV3Config.getMerchantId());
//        request.setOutTradeNo(orderNo);
//        // 3. 调用微信查询订单接口
//        Transaction transaction = service.queryOrderByOutTradeNo(request);
//        if (transaction != null) {
//            // 3.1 判断订单状态
//            if (Transaction.TradeStateEnum.SUCCESS == transaction.getTradeState()) {
//                // todo 3.2 看看实付的金额对不对
//                return true;
//            }
//        }
//        return false;

        // todo:由于我不能支付，所以这里模拟前端轮循到第四次返回订单支付成功
        String key = "pay_query_count:" + orderNo;
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 10, TimeUnit.MINUTES); // 设置过期时间

        if (count != null && count > 3) { // 假设轮询到第4次时模拟成功
            // 支付成功，我们直接去调用 updatePaymentInfo
            // 更新本地交易记录，包括状态，回调时间等信息，同时根据不同的类型：订单 | 充值 调用不同的远程服务，完成支付成功后的逻辑
            // 伪造一个交易信息
            Transaction transaction = new Transaction();
            transaction.setOutTradeNo(orderNo);
            transaction.setTransactionId("wx" + IdUtil.getSnowflakeNextId());
            paymentInfoService.updatePaymentInfo(transaction);
            return true;
        }
        return false;
    }


    /**
     * 微信支付成功后异步回调 todo:由于无法扫码，所以微信不会回调，只能写业务逻辑，没法测试。
     * @param request 请求对象
     * @return 返回结果
     * @throws Exception 异常
     */
    @Override
    public void wxPayNotify(HttpServletRequest request) {
       // 1. 验证签名，防止虚假通知
        //1.1 从请求头中获取封装请求参数对象数据
        String signature = request.getHeader("Wechatpay-Signature");
        String serial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String signaureType = request.getHeader("Wechatpay-Signature-Type");
        log.info("签名：{}，序列号：{}，随机数：{}，时间戳：{}，签名类型：{}", signature, serial, nonce, timestamp, signaureType);
        //1.2 获取请求体中参数
        String body = PayUtil.readData(request);
        log.info("请求体：{}", body);

        // 2. 解密
        //2.1 构造RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(body)
                .build();
        //2.2 初始化 NotificationParser解析器对象
        NotificationParser parser = new NotificationParser(rsaAutoCertificateConfig);
        //2.3 解密并转换成 Transaction交易对象
        Transaction transaction = parser.parse(requestParam, Transaction.class);
        log.info("交易对象：{}", transaction);

        // 3. 幂等性处理
        if(transaction != null){
            // 3.1 获取唯一标识
            String key = "payment:wxpay:" + transaction.getOutTradeNo();
            // 3.2 set nx
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "1", 24, TimeUnit.HOURS);
            // 3.3 第一次处理
            if(flag){
                try{
                    // 4. todo:核心业务处理
                    Transaction.TradeStateEnum tradeState = transaction.getTradeState();
                    if(Transaction.TradeStateEnum.SUCCESS == tradeState){
                        log.info("订单支付成功，处理核心业务");
                        // 4.1 更新本地交易记录，包括状态，回调时间等信息，同时根据不同的类型：订单 | 充值 调用不同的远程服务，完成支付成功后的逻辑
                        paymentInfoService.updatePaymentInfo(transaction);

                    }
                }catch (Exception e){
                    // 出现异常，删除缓存
                    redisTemplate.delete(key);
                    throw new RuntimeException(e);
                }
            }

        }

    }
}

