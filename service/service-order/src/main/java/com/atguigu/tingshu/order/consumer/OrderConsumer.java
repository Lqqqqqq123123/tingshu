package com.atguigu.tingshu.order.consumer;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单服务-消费者
 * @author liutianba7
 * @create 2025/12/24 17:10
 */
@Slf4j
@Component
public class OrderConsumer {

    @Autowired
    private OrderInfoService orderInfoService;
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_CANCEL_ORDER)
    public void cancelOrder(String orderNo, Channel channel, Message message)
    {
        log.info("订单服务消费者监听到延迟消息{}", orderNo);
        // todo: 幂等性的判断
        try {
            // 关闭订单
            orderInfoService.cancelOrder(orderNo);
            // 业务处理成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.info("订单服务消费者处理失败{}", orderNo);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            throw new RuntimeException(e);
        }
    }
}
