package org.example.aishop.common.constant;

/**
 * RabbitMQ 交换机、队列、路由键常量
 */
public interface MQConstant {

    // ==================== 统计模块 ====================
    /** 统计任务交换机 */
    String STATS_EXCHANGE = "aishop.stats.exchange";
    /** 统计任务队列 */
    String STATS_QUEUE = "aishop.stats.queue";
    /** 统计任务路由键 */
    String STATS_ROUTING_KEY = "aishop.stats.compute";

    // ==================== 商家入驻审核模块 ====================
    /** 审核后处理交换机 */
    String MERCHANT_AUDIT_EXCHANGE = "aishop.merchant.audit.exchange";
    /** 审核后处理队列 */
    String MERCHANT_AUDIT_QUEUE = "aishop.merchant.audit.queue";
    /** 审核后处理路由键 */
    String MERCHANT_AUDIT_ROUTING_KEY = "aishop.merchant.audit.after";

    // ==================== 售后模块 ====================
    /** 售后通知交换机 */
    String AFTERSALE_NOTIFY_EXCHANGE = "aishop.aftersale.notify.exchange";
    /** 售后通知队列 */
    String AFTERSALE_NOTIFY_QUEUE = "aishop.aftersale.notify.queue";
    /** 售后通知路由键 */
    String AFTERSALE_NOTIFY_ROUTING_KEY = "aishop.aftersale.notify";

    /** 售后延时交换机（死信交换机） */
    String AFTERSALE_DELAY_EXCHANGE = "aishop.aftersale.delay.exchange";
    /** 售后延时队列（带 TTL + 死信路由） */
    String AFTERSALE_DELAY_QUEUE = "aishop.aftersale.delay.queue";
    /** 售后延时路由键 */
    String AFTERSALE_DELAY_ROUTING_KEY = "aishop.aftersale.delay";

    /** 售后超时处理队列（死信消费队列） */
    String AFTERSALE_TIMEOUT_QUEUE = "aishop.aftersale.timeout.queue";
    /** 售后超时处理路由键 */
    String AFTERSALE_TIMEOUT_ROUTING_KEY = "aishop.aftersale.timeout";

    // ==================== 延时时间 ====================
    /** 退货超时自动关闭：7天（毫秒） */
    long AFTERSALE_RETURN_TIMEOUT_MS = 7 * 24 * 60 * 60 * 1000L;

    // ==================== 订单模块 ====================
    /** 订单创建交换机 */
    String ORDER_CREATE_EXCHANGE = "aishop.order.create.exchange";
    /** 订单创建队列（异步扣库存、生成流水） */
    String ORDER_CREATE_QUEUE = "aishop.order.create.queue";
    /** 订单创建路由键 */
    String ORDER_CREATE_ROUTING_KEY = "aishop.order.create";

    /** 订单延时交换机（死信交换机） */
    String ORDER_DELAY_EXCHANGE = "aishop.order.delay.exchange";
    /** 订单延时队列（30分钟 TTL） */
    String ORDER_DELAY_QUEUE = "aishop.order.delay.queue";
    /** 订单延时路由键 */
    String ORDER_DELAY_ROUTING_KEY = "aishop.order.delay";

    /** 订单超时关单队列（死信消费队列） */
    String ORDER_TIMEOUT_QUEUE = "aishop.order.timeout.queue";
    /** 订单超时关单路由键 */
    String ORDER_TIMEOUT_ROUTING_KEY = "aishop.order.timeout";

    /** 30分钟未支付自动关单（毫秒） */
    long ORDER_PAY_TIMEOUT_MS = 30 * 60 * 1000L;

    // ==================== 优惠券模块 ====================
    /** 优惠券通知交换机 */
    String COUPON_NOTIFY_EXCHANGE = "aishop.coupon.notify.exchange";
    /** 优惠券通知队列 */
    String COUPON_NOTIFY_QUEUE = "aishop.coupon.notify.queue";
    /** 优惠券通知路由键 */
    String COUPON_NOTIFY_ROUTING_KEY = "aishop.coupon.notify";

    /** 优惠券延时交换机（死信交换机） */
    String COUPON_DELAY_EXCHANGE = "aishop.coupon.delay.exchange";
    /** 优惠券延时队列（到期自动过期） */
    String COUPON_DELAY_QUEUE = "aishop.coupon.delay.queue";
    /** 优惠券延时路由键 */
    String COUPON_DELAY_ROUTING_KEY = "aishop.coupon.delay";

    /** 优惠券过期处理队列（死信消费队列） */
    String COUPON_EXPIRE_QUEUE = "aishop.coupon.expire.queue";
    /** 优惠券过期处理路由键 */
    String COUPON_EXPIRE_ROUTING_KEY = "aishop.coupon.expire";

    // ==================== 评论模块 ====================
    /** 评论异步处理交换机 */
    String COMMENT_EXCHANGE = "aishop.comment.exchange";
    /** 评论异步处理队列 */
    String COMMENT_QUEUE = "aishop.comment.queue";
    /** 评论异步处理路由键 */
    String COMMENT_ROUTING_KEY = "aishop.comment.process";

    // ==================== 销量同步模块 ====================
    /** 销量同步交换机 */
    String SALES_SYNC_EXCHANGE = "aishop.sales.sync.exchange";
    /** 销量同步队列 */
    String SALES_SYNC_QUEUE = "aishop.sales.sync.queue";
    /** 销量同步路由键 */
    String SALES_SYNC_ROUTING_KEY = "aishop.sales.sync";

    // ==================== 秒杀模块 ====================
    /** 秒杀下单交换机 */
    String SECKILL_EXCHANGE = "aishop.seckill.exchange";
    /** 秒杀下单队列 */
    String SECKILL_QUEUE = "aishop.seckill.queue";
    /** 秒杀下单路由键 */
    String SECKILL_ROUTING_KEY = "aishop.seckill.order";
}