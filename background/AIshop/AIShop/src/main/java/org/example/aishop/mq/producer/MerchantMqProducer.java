package org.example.aishop.mq.producer;

import lombok.RequiredArgsConstructor;
import org.example.aishop.mq.message.MerchantAuditMQMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static org.example.aishop.common.constant.MQConstant.*;

/**
 * 商家审核消息生产者
 *
 * 封装 RabbitTemplate，统一管理商家审核相关的 MQ 消息发送。
 * 业务 Service 中禁止直接使用 RabbitTemplate。
 */
@Component
@RequiredArgsConstructor
public class MerchantMqProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送商家入驻审核后处理消息（站内信通知等）
     */
    public void sendAuditAfter(MerchantAuditMQMessage mqMsg) {
        rabbitTemplate.convertAndSend(
                MERCHANT_AUDIT_EXCHANGE,
                MERCHANT_AUDIT_ROUTING_KEY,
                mqMsg);
    }
}