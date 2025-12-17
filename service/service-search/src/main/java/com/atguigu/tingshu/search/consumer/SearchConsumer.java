package com.atguigu.tingshu.search.consumer;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.search.AlbumUpdateStatVo;
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
 * @create 2025/12/13 11:48
 */
@Component
@Slf4j
public class SearchConsumer {

    @Autowired
    private SearchService searchService;
    @Autowired
    private RedisTemplate redisTemplate;
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

    /**
     * 监听更新专辑, 声音统计信息,然后如果当前声音的播放量或者评论数更新了,那么 ES 中存放的专辑信息也需要更新
     * @param vo
     * @param message
     * @param channel
     */
    // todo : 测试
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ALBUM_ES_STAT_UPDATE, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_TRACK),
            key = {MqConst.ROUTING_TRACK_STAT_UPDATE}
    ))
    @SneakyThrows
    public void updateTrackStat (TrackStatMqVo vo, Message message, Channel channel) {
        try {
            //更新专辑播放量
            log.info("service:监听更新专辑, 声音统计信息");
            // 保整幂等性
            String key = RedisConstant.BUSINESS_PREFIX + "es:" + vo.getBusinessNo();
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
            if(flag){
                // 2. 业务处理
                // 2.1 只有是播放量或者评论数更新了,那么 ES 中存放的专辑信息也需要更新
                if(vo.getStatType().equals(SystemConstant.TRACK_STAT_PLAY) || vo.getStatType().equals(SystemConstant.TRACK_STAT_COMMENT))
                {
                    // 2.2 创建 AlbumUpdateStatVo
                    AlbumUpdateStatVo albumUpdateStatVo = new AlbumUpdateStatVo();
                    albumUpdateStatVo.setAlbumId(vo.getAlbumId());
                    albumUpdateStatVo.setCount(vo.getCount());
                    // 2.3 如果是播放量更新了,那么封装 albumUpdateStatVo 的 StatType 为专辑播放量
                    if(vo.getStatType().equals(SystemConstant.TRACK_STAT_PLAY)){
                        albumUpdateStatVo.setStatType(SystemConstant.ALBUM_STAT_PLAY);
                    }

                    // 2.4 如果是评论数更新了,那么封装 albumUpdateStatVo 的 StatType 为专辑评论数
                    if(vo.getStatType().equals(SystemConstant.TRACK_STAT_COMMENT))
                    {
                        albumUpdateStatVo.setStatType(SystemConstant.ALBUM_STAT_COMMENT);
                    }

                    searchService.updateAlbumStat(albumUpdateStatVo);
                }

            }
            // 1.3 写入失败: 重复消息, 跳过
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }


}
