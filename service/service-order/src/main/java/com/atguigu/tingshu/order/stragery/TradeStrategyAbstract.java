package com.atguigu.tingshu.order.stragery;

import cn.hutool.core.bean.BeanUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 抽象类，为了提取不同策略类的公共方法
 * @author liutianba7
 * @create 2025/12/24 09:50
 */
@Component
public abstract class TradeStrategyAbstract implements TradeStrategy {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 生成订单流水号，用于防止订单重复提交
     * 通过Redis存储用户对应的流水号，设置5分钟过期时间
     *
     * @param vo 订单信息对象，用于设置生成的流水号
     * @param userId 用户ID，作为Redis key的前缀标识
     */
    public void generate_tradeNo(OrderInfoVo vo, Long userId){

        // 5.2 流水号：防止订单的重复提交 前缀:userId value-> 流水号值（uuid） ttl = 5min
        /**
         * 1. 网络卡顿，用户重复提交订单
         * 2. 成功提交订单，回退到订单确认页面，用户重复提交订单
         */
        // 5.2.1 生成流水号
        String key = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(key, tradeNo, 5, TimeUnit.MINUTES);
        vo.setTradeNo(tradeNo);
    }

    /**
     * 生成订单签名
     * 用于防止订单数据被篡改，确保数据完整性
     *
     * @param vo 订单信息对象，包含订单的基本信息
     */
    public void  generate_sign(OrderInfoVo vo)
    {
        // 5.4 签名：防止数据被篡改
        /**
         * 如果没有签名，当数据被修改，就会导致业务异常
         */
        // 5.4.1 生成签名：支付方式不能放进去，因为提交订单的时候是不知道用什么支付方式的，但是下单的时候，会选择支付方式，这就会导致签名一定不一致
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(vo, false, true);
        String sign = SignHelper.getSign(stringObjectMap);
        vo.setSign(sign);
    }

}
