package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.Login;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;


    /**
     * 下单或充值选择微信支付，返回小程序拉起微信支付所需参数
     * * @param paymentType 支付类型：1301下单  1302充值
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
    @Login
    @Operation(summary = "下单或充值选择微信支付，返回小程序拉起微信支付所需参数")
    @PostMapping("/wxPay/createJsapi/{paymentType}/{orderNo}")
    public Result<Map<String, String>> createJsapi(@PathVariable String paymentType,
                                                   @PathVariable String orderNo){
        Map<String, String> map = wxPayService.createJsapi(paymentType, orderNo);
        return Result.ok(map);
    }


    /**
     * 查询订单支付状态
     * @param orderNo 订单编号
     * @return true: 已支付，false: 未支付
     */
    @Login
    @Operation(summary = "商户端主动查询微信支付结果")
    @GetMapping("/wxPay/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo){
        boolean result = wxPayService.queryPayStatus(orderNo);
        return Result.ok(result);
    }

    /**
     * 微信支付成功后异步回调
     * @param request 请求对象
     * @return 返回结果
     * @throws Exception 异常
     */
    @Operation(summary = "微信支付成功后异步回调")
    @PostMapping("/wxPay/notify")
    public Result wxPayNotify(HttpServletRequest request) throws Exception {

        wxPayService.wxPayNotify(request);
        return Result.build(null, 200, "SUCCESS");
    }

}