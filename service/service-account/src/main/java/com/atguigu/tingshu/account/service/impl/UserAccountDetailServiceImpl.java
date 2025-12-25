package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.service.UserAccountDetailService;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author liutianba7
 * @create 2025/12/11 17:37
 */
@Service
public class UserAccountDetailServiceImpl extends ServiceImpl<UserAccountDetailMapper, UserAccountDetail> implements UserAccountDetailService {

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;
    /**
     * 根据交易类型分页查询账户变动日志
     * @param pageInfo 分页参数 | 分页返回账户变动日志
     * @param tradeType 查询的类型：充值 1201 | 消费 1204
     * @param userId 用户ID
     */
    @Override
    public void getUserAccountDetailPage(Page<UserAccountDetail> pageInfo, String tradeType, Long userId) {
        userAccountDetailMapper.getUserAccountDetailPage(pageInfo,tradeType,userId);
    }
}
