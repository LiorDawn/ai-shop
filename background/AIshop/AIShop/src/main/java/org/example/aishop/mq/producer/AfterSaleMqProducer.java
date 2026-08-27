package org.example.aishop.mq.producer;

import lombok.RequiredArgsConstructor;
import org.example.aishop.mq.message.AfterSaleMQMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static org.example.aishop.common.constant.MQConstant.*;

/**
 * 售后消息生产者
 *
 * 封装 RabbitTemplate，统一管理售后相关的 MQ 消息发送。
 * 业务 Service 中禁止直接使用 RabbitTemplate。
 */
@Component
@RequiredArgsConstructor
public class AfterSaleMqProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送售后通知消息（新申请、审核结果、完成通知）
     */
    public void sendNotify(AfterSaleMQMessage notifyMsg) {
        rabbitTemplate.convertAndSend(
                AFTERSALE_NOTIFY_EXCHANGE,
                AFTERSALE_NOTIFY_ROUTING_KEY,
                notifyMsg);
    }

    /**
     * 发送售后延时关闭消息（退货退款审核通过后，7 天超时未填物流自动关闭）
     */
    public void sendDelayClose(AfterSaleMQMessage delayMsg) {
        rabbitTemplate.convertAndSend(
                AFTERSALE_DELAY_EXCHANGE,
                AFTERSALE_DELAY_ROUTING_KEY,
                delayMsg);
    }
}