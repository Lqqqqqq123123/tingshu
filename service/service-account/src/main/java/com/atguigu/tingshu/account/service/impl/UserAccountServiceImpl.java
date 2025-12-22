package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.BusinessException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;
    @Autowired
    private UserAccountDetailMapper UserAccountDetailMapper;

    /**
     * 获取当前用户账户余额：必须登录才能访问
     * @param  userId: 用户ID
     * @return
     */
    @Override
    public BigDecimal getAvailableAmount(Long userId) {
        UserAccount userAccount = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
        if (userAccount == null) {
            // 抛出业务异常
            throw new BusinessException(ResultCodeEnum.DATA_QUERY_ERROR);
        }
        return userAccount.getAvailableAmount();
    }

    /**
     * 根据传入的参数 AccountDeductVo 扣减用户余额 + 记录操作日志
     * @param vo 扣减参数
     * @return 扣减结果
     */
    @Override
    public void checkAndDeduct(AccountDeductVo vo) {
        // 1. 先去获得当前用户的余额 用数据库来解决并发问题
        UserAccount userAccount = userAccountMapper.checkDeduction(vo.getUserId(), vo.getAmount());
        if(userAccount == null){
            throw new BusinessException(501, "余额不足");
        }
        // 2. 扣减账户余额
        int rows = userAccountMapper.update(null,
                new LambdaUpdateWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, vo.getUserId())
                        .setSql("total_amount = total_amount - " + vo.getAmount())
                        .setSql("available_amount = available_amount - " + vo.getAmount())
                        .setSql("total_pay_amount = total_pay_amount + " + vo.getAmount())
        );
        if(rows == 0){
            throw new BusinessException(502, "更新账户信息失败");
        }
        // 3. 添加操作日志
        UserAccountDetail detail = new UserAccountDetail();

        detail.setUserId(vo.getUserId());
        detail.setTitle(vo.getContent());
        detail.setAmount(vo.getAmount());
        detail.setOrderNo(vo.getOrderNo());
        detail.setTradeType(SystemConstant.ACCOUNT_TRADE_TYPE_MINUS);
        rows = UserAccountDetailMapper.insert(detail);
        if(rows == 0){
            throw new BusinessException(503, "添加操作日志失败");
        }

        log.info("用户账户扣减成功");
    }
}
