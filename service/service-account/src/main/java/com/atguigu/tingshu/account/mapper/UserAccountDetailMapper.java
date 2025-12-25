package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountDetailMapper extends BaseMapper<UserAccountDetail> {

    /**
     * 根据交易类型分页查询账户变动日志
     * @param pageInfo 分页参数 | 分页返回账户变动日志
     * @param tradeType 查询的类型：充值 1201 | 消费 1204
     * @param userId 用户ID
     * @return 账户变动日志
     */
    IPage <UserAccountDetail> getUserAccountDetailPage(Page<UserAccountDetail> pageInfo, @Param("tradeType") String tradeType, @Param("userId") Long userId);
}
