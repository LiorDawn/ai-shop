package org.example.aishop.mq.producer;

import lombok.RequiredArgsConstructor;
import org.example.aishop.mq.message.StatsMQMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static org.example.aishop.common.constant.MQConstant.*;

/**
 * 统计消息生产者
 *
 * 封装 RabbitTemplate，统一管理统计相关的 MQ 消息发送。
 * 业务 Service 中禁止直接使用 RabbitTemplate。
 */
@Component
@RequiredArgsConstructor
public class StatsMqProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送管理端概览统计消息
     */
    public void sendAdminOverview() {
        rabbitTemplate.convertAndSend(
                STATS_EXCHANGE,
                STATS_ROUTING_KEY,
                StatsMQMessage.adminOverview());
    }

    /**
     * 发送商家概览统计消息
     */
    public void sendMerchantOverview(Long shopId) {
        rabbitTemplate.convertAndSend(
                STATS_EXCHANGE,
                STATS_ROUTING_KEY,
                StatsMQMessage.merchantOverview(shopId));
    }

    /**
     * 发送销量排行统计消息
     */
    public void sendSalesRanking(Long shopId) {
        rabbitTemplate.convertAndSend(
                STATS_EXCHANGE,
                STATS_ROUTING_KEY,
                StatsMQMessage.salesRanking(shopId));
    }

    /**
     * 发送订单趋势统计消息
     */
    public void sendOrderTrend(Long shopId) {
        rabbitTemplate.convertAndSend(
                STATS_EXCHANGE,
                STATS_ROUTING_KEY,
                StatsMQMessage.orderTrend(shopId));
    }
}