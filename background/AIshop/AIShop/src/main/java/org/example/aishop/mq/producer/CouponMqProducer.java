package org.example.aishop.mq.producer;

import lombok.RequiredArgsConstructor;
import org.example.aishop.common.constant.MQConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 优惠券消息生产者
 *
 * 封装 RabbitTemplate，统一管理优惠券相关的 MQ 消息发送。
 * 业务 Service 中禁止直接使用 RabbitTemplate。
 */
@Component
@RequiredArgsConstructor
public class CouponMqProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送优惠券延时过期消息（到期自动置为过期状态）
     *
     * @param couponId 优惠券 ID
     * @param delayMs  延时毫秒数（endTime - now）
     */
    public void sendDelayExpire(Long couponId, long delayMs) {
        rabbitTemplate.convertAndSend(
                MQConstant.COUPON_DELAY_EXCHANGE,
                MQConstant.COUPON_DELAY_ROUTING_KEY,
                couponId,
                message -> {
                    message.getMessageProperties().setExpiration(String.valueOf(delayMs));
                    return message;
                });
    }

    /**
     * 发送领券通知消息（异步推送领券通知）
     *
     * @param userId 领取用户 ID
     */
    public void sendClaimNotify(Long userId) {
        rabbitTemplate.convertAndSend(
                MQConstant.COUPON_NOTIFY_EXCHANGE,
                MQConstant.COUPON_NOTIFY_ROUTING_KEY,
                userId);
    }
}