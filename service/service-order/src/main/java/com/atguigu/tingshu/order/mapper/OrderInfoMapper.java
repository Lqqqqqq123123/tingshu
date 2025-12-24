package com.atguigu.tingshu.order.mapper;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 获取订单列表
     * @param pageInfo 分页信息
     * @param userId 用户 id
     * @return 订单列表
     */
    Page<OrderInfo> getOrderByPage(Page<OrderInfo> pageInfo, @Param("userId") Long userId);
}
