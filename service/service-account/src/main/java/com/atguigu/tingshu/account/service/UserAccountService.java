package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    /**
     * 获取当前用户账户余额：必须登录才能访问
     * @param  userId: 用户ID
     * @return 账户余额
     */
    BigDecimal getAvailableAmount(Long userId);

    /**
     * 根据传入的参数 AccountDeductVo 扣减用户余额 + 记录操作日志
     * @param vo 扣减参数
     * @return 扣减结果
     */
    void checkAndDeduct(AccountDeductVo vo);

}
