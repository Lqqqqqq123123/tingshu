package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    /**
     * 获取当前用户账户余额：必须登录才能访问
     * @param  userId: 用户ID
     * @return 账户余额
     */
    BigDecimal getAvailableAmount(Long userId);
}
