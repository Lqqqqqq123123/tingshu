package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author liutianba7
 * @create 2025/12/11 17:37
 */

public interface UserAccountDetailService extends IService<UserAccountDetail> {

    /**
     * 根据交易类型分页查询账户变动日志
     * @param pageInfo 分页参数 | 分页返回账户变动日志
     * @param tradeType 查询的类型：充值 1201 | 消费 1204
     * @param userId 用户ID
     */
    void getUserAccountDetailPage(Page<UserAccountDetail> pageInfo, String tradeType, Long userId);
}
