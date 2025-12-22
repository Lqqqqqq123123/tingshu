package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 检测用户余额是否充足
     * @param userId 用户id
     * @param amount 扣减金额
     * @return 用户账户信息
     */
    UserAccount checkDeduction(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
