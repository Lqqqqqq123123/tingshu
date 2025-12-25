package com.atguigu.tingshu.account.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * 配置类，完成取消充值订单的 rmq 的配置
 * @author liutianba7
 * @create 2025/12/25 14:34
 */

@Configuration
public class CancelRechargeConfiguration {

    @Bean
    public CustomExchange cancelRechargeExchange() {
        HashMap<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange("cancel_recharge_exchange", "x-delayed-message", true, false, args);
    }

    @Bean
    public Queue cancelRechargequeue() {
        return new Queue("cancel_recharge_queue", true);
    }

    @Bean
    public Binding cancelRechargeBinding() {
        return BindingBuilder.bind(cancelRechargequeue()).to(cancelRechargeExchange()).with("cancel_recharge_routing").noargs();
    }
}
