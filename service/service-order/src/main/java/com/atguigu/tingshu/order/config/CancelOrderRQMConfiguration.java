package com.atguigu.tingshu.order.config;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * 订单服务RabbitMQ配置类:实现延迟一定时间后，如果订单还没有支付成功，就去关闭订单
 * @author liutianba7
 * @create 2025/12/24 16:54
 */
@Configuration
public class CancelOrderRQMConfiguration {
    @Bean
    public CustomExchange exchange(){
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_ORDER, "x-delayed-message", true, false, arguments);
    }


    @Bean
    public Queue queue(){
        return new Queue(MqConst.QUEUE_CANCEL_ORDER, true);
    }

    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue()).to(exchange()).with(MqConst.ROUTING_CANCEL_ORDER).noargs();
    }
}
