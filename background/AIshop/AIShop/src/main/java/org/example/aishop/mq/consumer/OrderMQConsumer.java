package org.example.aishop.mq.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.entity.order.OrderItem;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.product.ProductSku;
import org.example.aishop.mapper.order.OrderItemMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.ProductSkuMapper;
import org.example.aishop.mq.message.OrderCreateMQMessage;
import org.example.aishop.mq.message.OrderCreateMQMessage.OrderItemEntry;
import org.example.aishop.mq.message.OrderTimeoutMQMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Collections;
import java.util.List;

/**
 * 订单 MQ 消费者
 * 异步扣减 MySQL 库存、生成订单流水，投递延时关单消息
 *
 * 设计要点：
 * 1. 幂等：消费前先查订单是否存在，存在则直接 ACK 跳过
 * 2. 手动 ACK：全部业务成功后 basicAck，失败时区分处理
 * 3. 不可重试异常（主键冲突等）NACK 不重回队列
 * 4. 核心逻辑只有订单入库 + 库存扣减，不夹杂统计查询
 */
@Component
public class OrderMQConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderMQConsumer.class);

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductSkuMapper productSkuMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("rollbackStockScript")
    private RedisScript<Long> rollbackStockScript;

    /**
     * 消费订单创建消息，异步持久化 MySQL
     *
     * 幂等保证：先查订单是否存在，存在直接 ACK 跳过重复消费
     * 手动 ACK：Channel 参数由 Spring AMQP 自动注入
     */
    @RabbitListener(queues = MQConstant.ORDER_CREATE_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void onCreateOrder(OrderCreateMQMessage msg, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        if (msg == null || msg.getOrderId() == null) {
            basicAck(channel, tag);
            return;
        }

        try {
            // ==================== 幂等校验 ====================
            // 订单已存在 → 重复投递，直接确认跳过
            Orders existing = orderMapper.selectById(msg.getOrderId());
            if (existing != null) {
                log.warn("订单 " + msg.getOrderNo() + " 已存在，跳过重复消费（orderId=" + msg.getOrderId() + "）");
                basicAck(channel, tag);
                return;
            }

            // ==================== 1. 保存订单主表 ====================
            Orders order = new Orders();
            order.setId(msg.getOrderId());
            order.setOrderNo(msg.getOrderNo());
            order.setUserId(msg.getUserId());
            order.setTotalPrice(msg.getTotalPrice());
            order.setCouponPrice(msg.getCouponPrice());
            order.setActualPrice(msg.getActualPrice());
            order.setPayType(0);
            order.setPayStatus(0);
            order.setOrderStatus(0);
            order.setAddressId(msg.getAddressId());
            order.setRemark(msg.getRemark());
            orderMapper.insert(order);

            // ==================== 2. 保存订单明细 + 扣减 MySQL 库存 ====================
            StringBuilder stockSnapshot = new StringBuilder("[");
            for (int i = 0; i < msg.getItems().size(); i++) {
                OrderItemEntry entry = msg.getItems().get(i);
                OrderItem item = new OrderItem();
                item.setOrderId(msg.getOrderId());
                item.setProductId(entry.getProductId());
                item.setSkuId(entry.getSkuId());
                item.setShopId(entry.getShopId());
                item.setNum(entry.getNum());
                item.setPrice(entry.getPrice());
                orderItemMapper.insert(item);

                // 扣减 MySQL SKU 库存
                ProductSku sku = productSkuMapper.selectById(entry.getSkuId());
                if (sku != null) {
                    sku.setStock(sku.getStock() - entry.getNum());
                    productSkuMapper.updateById(sku);
                }

                if (i > 0) stockSnapshot.append(",");
                stockSnapshot.append("{\"skuId\":").append(entry.getSkuId())
                        .append(",\"num\":").append(entry.getNum()).append("}");
            }
            stockSnapshot.append("]");

            // ==================== 3. 投递延时关单消息 ====================
            // 30 分钟未支付自动关单（由 MQ TTL 机制触发）
            OrderTimeoutMQMessage timeoutMsg = new OrderTimeoutMQMessage();
            timeoutMsg.setOrderId(msg.getOrderId());
            timeoutMsg.setUserId(msg.getUserId());
            timeoutMsg.setStockSnapshot(stockSnapshot.toString());
            timeoutMsg.setCouponId(msg.getCouponId());
            rabbitTemplate.convertAndSend(MQConstant.ORDER_DELAY_EXCHANGE,
                    MQConstant.ORDER_DELAY_ROUTING_KEY, timeoutMsg);

            // ==================== 全部成功 → 手动 ACK ====================
            basicAck(channel, tag);
            log.info("订单 " + msg.getOrderNo() + " 异步创建成功，已投递延时关单消息");

        } catch (DuplicateKeyException e) {
            // 主键冲突：业务永久失败，NACK 不重回队列，避免无限重试
            log.error("订单 " + msg.getOrderNo() + " 主键冲突，丢弃消息（无需重试）", e);
            basicNack(channel, tag, false);
            // 标记事务回滚（DB 操作已失败）
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

        } catch (Exception e) {
            // 其他异常（网络超时、DB 连接断开等）：NACK 重回队列，由 Spring retry 重试
            log.error("订单创建消费失败 orderId=" + msg.getOrderId() + "，将重试", e);
            basicNack(channel, tag, true);
            // 抛出异常触发 @Transactional 回滚，同时 Spring retry 会捕获并重试
            throw e;
        }
    }

    /**
     * 消费延时关单消息（30 分钟 TTL 到期后自动触发）
     * 手动 ACK + 幂等：订单已取消或不存在直接跳过
     */
    @RabbitListener(queues = MQConstant.ORDER_TIMEOUT_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void onTimeoutClose(OrderTimeoutMQMessage msg, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        if (msg == null || msg.getOrderId() == null) {
            basicAck(channel, tag);
            return;
        }

        try {
            Orders order = orderMapper.selectById(msg.getOrderId());
            // 订单不存在或已支付/已取消 → 幂等跳过
            if (order == null || order.getOrderStatus() != 0) {
                basicAck(channel, tag);
                return;
            }

            // 关闭订单
            order.setOrderStatus(4); // 已取消
            orderMapper.updateById(order);

            // 回滚 Redis 库存
            if (msg.getStockSnapshot() != null) {
                try {
                    List<StockSnapshot> snapshots = objectMapper.readValue(
                            msg.getStockSnapshot(), new TypeReference<List<StockSnapshot>>() {});
                    for (StockSnapshot ss : snapshots) {
                        String skuKey = RedisConstant.stockSkuKey(ss.getSkuId());
                        String couponKey = "";
                        if (msg.getCouponId() != null && msg.getCouponId() > 0) {
                            couponKey = RedisConstant.couponStockKey(msg.getCouponId());
                        }
                        stringRedisTemplate.execute(rollbackStockScript,
                                Collections.singletonList(skuKey),
                                String.valueOf(ss.getNum()));
                    }
                } catch (Exception ignored) {
                    log.warn("库存回滚失败 orderId=" + msg.getOrderId());
                }
            }

            basicAck(channel, tag);
            log.info("订单 " + msg.getOrderId() + " 超时未支付，已自动关闭");

        } catch (Exception e) {
            log.error("超时关单消费失败 orderId=" + msg.getOrderId(), e);
            basicNack(channel, tag, true);
            throw e;
        }
    }

    // ==================== 手动 ACK 工具方法 ====================

    private void basicAck(Channel channel, long tag) {
        try {
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.warn("basicAck 失败 tag=" + tag, e);
        }
    }

    private void basicNack(Channel channel, long tag, boolean requeue) {
        try {
            channel.basicNack(tag, false, requeue);
        } catch (Exception e) {
            log.warn("basicNack 失败 tag=" + tag + ", requeue=" + requeue, e);
        }
    }

    /** 库存快照辅助类 */
    @lombok.Data
    static class StockSnapshot {
        private Long skuId;
        private Integer num;
    }
}