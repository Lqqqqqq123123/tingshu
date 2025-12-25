package com.atguigu.tingshu.account.impl;


import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountDegradeFeignClient implements AccountFeignClient {

    @Override
    public Result CheckAndDeduct(AccountDeductVo vo) {
        log.info("远程调用账户服务 【CheckAndDeduct】 出现降级");
        return null;
    }

    @Override
    public Result<RechargeInfo> getRechargeInfo(String orderNo) {
        log.info("远程调用账户服务 【getRechargeInfo】 出现降级");
        return null;
    }

    @Override
    public Result rechargePaySuccess(String orderNo) {
        log.info("远程调用账户服务 【rechargePaySuccess】 出现降级");
        return null;
    }

    @Override
    public Result saveAccountDetail(UserAccountDetail userAccountDetail) {
        log.info("远程调用账户服务 【saveAccountDetail】 出现降级");
        return null;
    }
}
