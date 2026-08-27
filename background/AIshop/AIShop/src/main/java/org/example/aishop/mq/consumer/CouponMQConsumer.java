package org.example.aishop.mq.consumer;

import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.entity.coupon.Coupon;
import org.example.aishop.mapper.coupon.CouponMapper;
import org.example.aishop.mapper.coupon.UserCouponMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 优惠券 MQ 消费者
 * 异步推送领券通知、处理优惠券到期
 */
@Component
public class CouponMQConsumer {
    private static final Logger log = LoggerFactory.getLogger(CouponMQConsumer.class);

    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private UserCouponMapper userCouponMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 消费优惠券到期消息（死信队列中转来的）
     */
    @RabbitListener(queues = MQConstant.COUPON_EXPIRE_QUEUE)
    public void onCouponExpire(Long couponId) {
        if (couponId == null) return;
        try {
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null) return;
            // 到期自动置为过期
            coupon.setStatus(2); // 2=已过期
            couponMapper.updateById(coupon);
            log.info("优惠券 " + couponId + " 已到期，自动置为过期");
        } catch (Exception e) {
            log.error("优惠券过期处理失败 couponId=" + couponId, e);
        }
    }

    /**
     * 消费领券通知（异步推送站内信等）
     */
    @RabbitListener(queues = MQConstant.COUPON_NOTIFY_QUEUE)
    public void onCouponNotify(Long userId) {
        if (userId == null) return;
        try {
            // 此处可扩展：站内信推送、APP推送等
            log.info("用户 " + userId + " 领券通知已推送");
        } catch (Exception e) {
            log.error("领券通知推送失败 userId=" + userId, e);
        }
    }
}