package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 保存充值
     * @param rechargeInfoVo
     * @return {orderNo:"充值订单编号"}
     */
    Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo);

    /**
     * 	微信支付成功后，处理充值业务
     * @param orderNo
     * @return
     */
    void rechargePaySuccess(String orderNo);
}
