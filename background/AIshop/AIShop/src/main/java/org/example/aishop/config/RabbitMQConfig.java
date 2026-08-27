package org.example.aishop.config;

import org.example.aishop.common.constant.MQConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 交换机、队列、绑定配置
 */
@Configuration
public class RabbitMQConfig {

    /** JSON 消息转换器 */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ==================== 统计模块 ====================

    @Bean
    public DirectExchange statsExchange() {
        return new DirectExchange(MQConstant.STATS_EXCHANGE);
    }

    @Bean
    public Queue statsQueue() {
        return QueueBuilder.durable(MQConstant.STATS_QUEUE).build();
    }

    @Bean
    public Binding statsBinding() {
        return BindingBuilder.bind(statsQueue()).to(statsExchange()).with(MQConstant.STATS_ROUTING_KEY);
    }

    // ==================== 商家入驻审核模块 ====================

    @Bean
    public DirectExchange merchantAuditExchange() {
        return new DirectExchange(MQConstant.MERCHANT_AUDIT_EXCHANGE);
    }

    @Bean
    public Queue merchantAuditQueue() {
        return QueueBuilder.durable(MQConstant.MERCHANT_AUDIT_QUEUE).build();
    }

    @Bean
    public Binding merchantAuditBinding() {
        return BindingBuilder.bind(merchantAuditQueue()).to(merchantAuditExchange())
                .with(MQConstant.MERCHANT_AUDIT_ROUTING_KEY);
    }

    // ==================== 售后通知模块 ====================

    @Bean
    public DirectExchange aftersaleNotifyExchange() {
        return new DirectExchange(MQConstant.AFTERSALE_NOTIFY_EXCHANGE);
    }

    @Bean
    public Queue aftersaleNotifyQueue() {
        return QueueBuilder.durable(MQConstant.AFTERSALE_NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding aftersaleNotifyBinding() {
        return BindingBuilder.bind(aftersaleNotifyQueue()).to(aftersaleNotifyExchange())
                .with(MQConstant.AFTERSALE_NOTIFY_ROUTING_KEY);
    }

    // ==================== 售后延时关闭模块（死信队列实现延时） ====================

    /** 延时交换机 */
    @Bean
    public DirectExchange aftersaleDelayExchange() {
        return new DirectExchange(MQConstant.AFTERSALE_DELAY_EXCHANGE);
    }

    /**
     * 延时队列：设置 TTL 和死信路由
     * 消息在此队列中存活 TTL 时间后，自动转移到死信队列
     */
    @Bean
    public Queue aftersaleDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MQConstant.AFTERSALE_DELAY_EXCHANGE);
        args.put("x-dead-letter-routing-key", MQConstant.AFTERSALE_TIMEOUT_ROUTING_KEY);
        args.put("x-message-ttl", MQConstant.AFTERSALE_RETURN_TIMEOUT_MS);
        return QueueBuilder.durable(MQConstant.AFTERSALE_DELAY_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding aftersaleDelayBinding() {
        return BindingBuilder.bind(aftersaleDelayQueue()).to(aftersaleDelayExchange())
                .with(MQConstant.AFTERSALE_DELAY_ROUTING_KEY);
    }

    /** 超时处理队列（死信消费队列） */
    @Bean
    public Queue aftersaleTimeoutQueue() {
        return QueueBuilder.durable(MQConstant.AFTERSALE_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding aftersaleTimeoutBinding() {
        return BindingBuilder.bind(aftersaleTimeoutQueue()).to(aftersaleDelayExchange())
                .with(MQConstant.AFTERSALE_TIMEOUT_ROUTING_KEY);
    }

    // ==================== 订单模块 ====================

    @Bean
    public DirectExchange orderCreateExchange() {
        return new DirectExchange(MQConstant.ORDER_CREATE_EXCHANGE);
    }

    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(MQConstant.ORDER_CREATE_QUEUE).build();
    }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue()).to(orderCreateExchange())
                .with(MQConstant.ORDER_CREATE_ROUTING_KEY);
    }

    /** 订单延时交换机 */
    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(MQConstant.ORDER_DELAY_EXCHANGE);
    }

    /** 订单延时队列（30分钟 TTL） */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        /**当前队列的消息过期 / 被拒绝后，自动转发到哪个交换机  消息转发到死信交换机时，使用的路由键*/
        args.put("x-dead-letter-exchange", MQConstant.ORDER_DELAY_EXCHANGE);
        args.put("x-dead-letter-routing-key", MQConstant.ORDER_TIMEOUT_ROUTING_KEY);
        args.put("x-message-ttl", MQConstant.ORDER_PAY_TIMEOUT_MS);
        return QueueBuilder.durable(MQConstant.ORDER_DELAY_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange())
                .with(MQConstant.ORDER_DELAY_ROUTING_KEY);
    }

    /** 订单超时关单队列 */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(MQConstant.ORDER_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderDelayExchange())
                .with(MQConstant.ORDER_TIMEOUT_ROUTING_KEY);
    }

    // ==================== 优惠券模块 ====================

    @Bean
    public DirectExchange couponNotifyExchange() {
        return new DirectExchange(MQConstant.COUPON_NOTIFY_EXCHANGE);
    }

    @Bean
    public Queue couponNotifyQueue() {
        return QueueBuilder.durable(MQConstant.COUPON_NOTIFY_QUEUE).build();
    }

    @Bean
    public Binding couponNotifyBinding() {
        return BindingBuilder.bind(couponNotifyQueue()).to(couponNotifyExchange())
                .with(MQConstant.COUPON_NOTIFY_ROUTING_KEY);
    }

    /** 优惠券延时交换机 */
    @Bean
    public DirectExchange couponDelayExchange() {
        return new DirectExchange(MQConstant.COUPON_DELAY_EXCHANGE);
    }

    /** 优惠券延时队列（消息 TTL 由发送时设置） */
    @Bean
    public Queue couponDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MQConstant.COUPON_DELAY_EXCHANGE);
        args.put("x-dead-letter-routing-key", MQConstant.COUPON_EXPIRE_ROUTING_KEY);
        return QueueBuilder.durable(MQConstant.COUPON_DELAY_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding couponDelayBinding() {
        return BindingBuilder.bind(couponDelayQueue()).to(couponDelayExchange())
                .with(MQConstant.COUPON_DELAY_ROUTING_KEY);
    }

    /** 优惠券过期处理队列 */
    @Bean
    public Queue couponExpireQueue() {
        return QueueBuilder.durable(MQConstant.COUPON_EXPIRE_QUEUE).build();
    }

    @Bean
    public Binding couponExpireBinding() {
        return BindingBuilder.bind(couponExpireQueue()).to(couponDelayExchange())
                .with(MQConstant.COUPON_EXPIRE_ROUTING_KEY);
    }

    // ==================== 评论模块 ====================

    @Bean
    public DirectExchange commentExchange() {
        return new DirectExchange(MQConstant.COMMENT_EXCHANGE);
    }

    @Bean
    public Queue commentQueue() {
        return QueueBuilder.durable(MQConstant.COMMENT_QUEUE).build();
    }

    @Bean
    public Binding commentBinding() {
        return BindingBuilder.bind(commentQueue()).to(commentExchange())
                .with(MQConstant.COMMENT_ROUTING_KEY);
    }

    // ==================== 销量同步模块 ====================

    @Bean
    public DirectExchange salesSyncExchange() {
        return new DirectExchange(MQConstant.SALES_SYNC_EXCHANGE);
    }

    @Bean
    public Queue salesSyncQueue() {
        return QueueBuilder.durable(MQConstant.SALES_SYNC_QUEUE).build();
    }

    @Bean
    public Binding salesSyncBinding() {
        return BindingBuilder.bind(salesSyncQueue()).to(salesSyncExchange())
                .with(MQConstant.SALES_SYNC_ROUTING_KEY);
    }

    // ==================== 秒杀模块 ====================

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(MQConstant.SECKILL_EXCHANGE);
    }

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(MQConstant.SECKILL_QUEUE).build();
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange())
                .with(MQConstant.SECKILL_ROUTING_KEY);
    }
}