package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.BusinessException;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

	@Autowired
	private RechargeInfoMapper rechargeInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    /**
     * 保存充值
     * @param rechargeInfoVo
     * @return {orderNo:"充值订单编号"}
     */
    @Override
    public Map<String, String> submitRecharge(RechargeInfoVo rechargeInfoVo) {
        RechargeInfo po = new RechargeInfo();
        po.setUserId(AuthContextHolder.getUserId());
        String orderNo = "CZ" + DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        po.setOrderNo(orderNo);
        po.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        po.setRechargeAmount(rechargeInfoVo.getAmount());
        po.setPayWay(rechargeInfoVo.getPayWay());
        rechargeInfoMapper.insert(po);

        // todo 使用 rmq 的延迟消息实现5分钟如果还没支付，则取消该充值订单
        rabbitService.sendDealyMessage("cancel_recharge_exchange", "cancel_recharge_routing", po.getOrderNo(), 20); // 暂时20s，便于调试
        return Map.of("orderNo", po.getOrderNo());
    }

    /**
     * 	微信支付成功后，处理充值业务
     * @param orderNo
     * @return
     */
    @Override
    public void rechargePaySuccess(String orderNo) {
        // 1. 获取充值信息，
        RechargeInfo po = rechargeInfoMapper.selectOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo));
        Assert.notNull(po, "充值信息不存在！");
        // 2. 修改充值信息状态
        // if(rechargeStatus.equals(SystemConstant.ORDER_STATUS_CANCEL)) // 就算取消了，我们也要赋值为已支付
        po.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
        rechargeInfoMapper.updateById(po);

        // 3. 充值余额，也就是修改用户的账户信息
        boolean success = userAccountService.update(
                new LambdaUpdateWrapper<UserAccount>().eq(UserAccount::getUserId, po.getUserId())
                        .setSql("total_amount = total_amount + " + po.getRechargeAmount())
                        .setSql("available_amount = available_amount + " + po.getRechargeAmount())
                        .setSql("total_income_amount = total_income_amount + " + po.getRechargeAmount())
        );

        if(!success){
            log.info("rechargePaySuccess：充值失败");
            throw new BusinessException(503, "充值失败");
        }

        // 4. 添加变更日志
        UserAccountDetail detail = new UserAccountDetail();

        detail.setUserId(po.getUserId());
        detail.setTitle("余额充值：" + po.getRechargeAmount());
        detail.setAmount(po.getRechargeAmount());
        detail.setOrderNo(po.getOrderNo());
        detail.setTradeType(SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT);
        int rows = userAccountDetailMapper.insert(detail);
        if(rows == 0){
            throw new BusinessException(503, "添加操作日志失败");
        }

        log.info("用户账户充值成功");
    }
}
