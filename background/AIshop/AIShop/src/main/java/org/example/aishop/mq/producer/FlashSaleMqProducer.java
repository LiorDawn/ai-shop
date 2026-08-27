package org.example.aishop.mq.producer;

import lombok.RequiredArgsConstructor;
import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.mq.message.FlashSaleMQMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀消息生产者
 *
 * 封装 RabbitTemplate，统一管理秒杀相关的 MQ 消息发送。
 * 业务 Service 中禁止直接使用 RabbitTemplate。
 */
@Component
@RequiredArgsConstructor
public class FlashSaleMqProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送秒杀下单消息（异步处理秒杀订单创建）
     */
    public void sendSeckillOrder(FlashSaleMQMessage msg) {
        rabbitTemplate.convertAndSend(
                MQConstant.SECKILL_EXCHANGE,
                MQConstant.SECKILL_ROUTING_KEY,
                msg);
    }
}