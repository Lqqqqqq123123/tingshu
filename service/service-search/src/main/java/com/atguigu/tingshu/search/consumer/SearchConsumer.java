package com.atguigu.tingshu.search.consumer;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.search.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author liutianba7
 * @create 2025/12/13 11:48
 */
@Component
@Slf4j
public class SearchConsumer {

    @Autowired
    private SearchService searchService;
    /**
     * 接收到消息，然后调用专辑服务，将专辑信息保存到ES中
     * @param id
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ALBUM_UPPER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM),
            key = MqConst.ROUTING_ALBUM_UPPER
    ))
    public void save(Long id, Channel channel, Message message) {
        searchService.upperAlbum(id);
        log.info("保存专辑信息到ES中：{}", id);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
    }


    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ALBUM_LOWER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM, durable = "true"),
            key = MqConst.ROUTING_ALBUM_LOWER
    ))
    public void delete(Long id, Channel channel, Message message) {
        searchService.lowerAlbum(id);
        log.info("删除专辑信息：{}", id);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
    }
}
