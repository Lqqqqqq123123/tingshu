package com.atguigu.tingshu.album.consumer;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author liutianba7
 * @create 2025/12/17 15:52
 */
@Component
@Slf4j
public class AlbumConsumer {


    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private TrackInfoService trackInfoService;
    @Autowired
    private AlbumInfoService albumInfoService;


    /**
     * 监听更新专辑, 声音统计信息
     * @param vo
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TRACK_STAT_UPDATE, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_TRACK),
            key = {MqConst.ROUTING_TRACK_STAT_UPDATE}
    ))
    @SneakyThrows
    public void updateTrackStat(TrackStatMqVo vo, Message message, Channel channel) {
        try {
            //更新声音播放量
            log.info("监听到[更新专辑, 声音统计]信息");

            // 1. 幂等性处理(由于网络问题,可能会导致重复消息)
            // 1.1 获取消息的唯一标识
            String key = RedisConstant.BUSINESS_PREFIX + "db:" + vo.getBusinessNo();
            // 1.2 用 set nx 写入 redis, 写入成功才继续执行 (过期时间设置为10分钟即可)
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
            if(flag){
                // 2. 业务处理
                trackInfoService.updateStat(vo);
            }
            // 1.3 写入失败: 重复消息, 跳过
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }

    }

}
