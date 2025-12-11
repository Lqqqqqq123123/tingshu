package com.atguigu.tingshu.account.consumer;

import com.atguigu.tingshu.account.service.UserAccountDetailService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * @author liutianba7
 * @create 2025/12/11 17:27
 */

@Slf4j
@Component
public class AccountConsumer {

    /**
     * 监听用户注册成功，然后去初始化用户的账户信息
     * @param
     */
    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserAccountDetailService userAccountDetailService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "tingshu.account.queue"),
            exchange = @Exchange(MqConst.EXCHANGE_USER),
            key = {MqConst.ROUTING_USER_REGISTER}
    ))
    @Transactional
    @SneakyThrows
    public void initAccount(HashMap<String, Object> message, Channel channel, Message msg) {

        /*
            HashMap<String, Object> message = new HashMap<>();
            message.put("userId", info.getId());
            message.put("amount", 1000);
            message.put("orderNo", "free_amount" + DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId());
            message.put("title", "首次使用，赠送余额100元");
         */

        if(!CollectionUtils.isEmpty(message)){
            Long userId = (Long) message.get("userId");
            BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(message.get("amount").toString()));
            String orderNo = (String) message.get("orderNo");
            String title = (String) message.get("title");


            log.info("用户注册成功，初始化账户信息");
            // 账户信息
            UserAccount po = new UserAccount();

            po.setUserId(userId);
            po.setTotalAmount(amount);
            po.setAvailableAmount(amount);

            // 日志信息
            UserAccountDetail detail = new UserAccountDetail();

            detail.setUserId(userId);
            detail.setTitle(title);
            detail.setAmount(amount);
            detail.setOrderNo(orderNo);

            // 保存信息
            userAccountService.save(po);
            userAccountDetailService.save(detail);

            // 手动确认
            channel.basicAck(msg.getMessageProperties().getDeliveryTag(), true);
        }

    }
}
