package org.example.aishop.mq.producer;

import lombok.RequiredArgsConstructor;
import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.mq.message.CommentMQMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 评价消息生产者
 *
 * 封装 RabbitTemplate，统一管理评价相关的 MQ 消息发送。
 * 业务 Service 中禁止直接使用 RabbitTemplate。
 */
@Component
@RequiredArgsConstructor
public class CommentMqProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送评论异步处理消息（新增评论后触发统计、通知等）
     */
    public void sendCommentProcess(CommentMQMessage mqMsg) {
        rabbitTemplate.convertAndSend(
                MQConstant.COMMENT_EXCHANGE,
                MQConstant.COMMENT_ROUTING_KEY,
                mqMsg);
    }
}