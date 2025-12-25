package com.atguigu.tingshu.account;

import com.atguigu.tingshu.account.impl.AccountDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 账号模块远程调用API接口
 * </p>
 *
 * @author liutianba7
 */
@FeignClient(value = "service-account", fallback = AccountDegradeFeignClient.class, path = "/api/account")
public interface AccountFeignClient {
    /**
     * 根据传入的参数 AccountDeductVo 扣减用户余额 + 记录操作日志 内部接口：订单服务调用
     * @param vo 扣减参数
     * @return 扣减结果
     */
    @PostMapping("/userAccount/checkAndDeduct")
    public Result CheckAndDeduct (@RequestBody AccountDeductVo vo);


    /**
     * 根据订单号查询充值信息
     * @param orderNo 订单号
     * @return 充值信息
     */
    @GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
    public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo);

    /**
     * 	微信支付成功后，处理充值业务 供支付接口调用
     * @param orderNo 订单号
     * @return
     */
    @GetMapping("/rechargeInfo/rechargePaySuccess/{orderNo}")
    public Result rechargePaySuccess(@PathVariable String orderNo);


    /**
     * 保存账户流水
     * @param userAccountDetail
     * @return
     */
    @GetMapping("userAccount/saveAccountDetail")
    public Result saveAccountDetail(UserAccountDetail userAccountDetail);
}
