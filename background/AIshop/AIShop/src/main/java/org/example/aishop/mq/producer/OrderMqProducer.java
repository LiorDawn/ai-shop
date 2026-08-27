package org.example.aishop.mq.producer;

import lombok.RequiredArgsConstructor;
import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.mq.message.OrderTimeoutMQMessage;
import org.example.aishop.mq.message.SalesSyncMQMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者
 *
 * 封装 RabbitTemplate，统一管理订单相关的 MQ 消息发送。
 * 业务 Service 中禁止直接使用 RabbitTemplate。
 */
@Component
@RequiredArgsConstructor
public class OrderMqProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单延时关单消息（30 分钟超时未支付自动关闭）
     */
    public void sendOrderDelayClose(OrderTimeoutMQMessage timeoutMsg) {
        rabbitTemplate.convertAndSend(
                MQConstant.ORDER_DELAY_EXCHANGE,
                MQConstant.ORDER_DELAY_ROUTING_KEY,
                timeoutMsg);
    }

    /**
     * 发送销量同步消息（确认收货后增量更新商品销量）
     */
    public void sendSalesSync(SalesSyncMQMessage syncMsg) {
        rabbitTemplate.convertAndSend(
                MQConstant.SALES_SYNC_EXCHANGE,
                MQConstant.SALES_SYNC_ROUTING_KEY,
                syncMsg);
    }
}