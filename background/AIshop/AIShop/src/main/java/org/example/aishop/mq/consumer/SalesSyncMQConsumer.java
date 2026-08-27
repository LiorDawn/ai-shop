package org.example.aishop.mq.consumer;

import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.entity.product.Product;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mq.message.SalesSyncMQMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 销量同步 MQ 消费者
 * 批量将 Redis 增量销量同步到 MySQL product.sales 字段
 */
@Component
public class SalesSyncMQConsumer {
    private static final Logger log = LoggerFactory.getLogger(SalesSyncMQConsumer.class);

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 消费销量同步消息，将 Redis 增量写入 MySQL
     */
    @RabbitListener(queues = MQConstant.SALES_SYNC_QUEUE)
    public void onSalesSync(SalesSyncMQMessage msg) {
        if (msg == null || msg.getProductId() == null) return;
        try {
            String key = "AISHOP:SALES:DELTA:" + msg.getProductId();
            String deltaStr = stringRedisTemplate.opsForValue().get(key);
            int delta = 0;
            if (deltaStr != null) {
                delta = Integer.parseInt(deltaStr);
            }

            if (delta > 0) {
                Product product = productMapper.selectById(msg.getProductId());
                if (product != null) {
                    product.setSales(product.getSales() + delta);
                    productMapper.updateById(product);
                    // 同步完成后清除增量计数
                    stringRedisTemplate.delete(key);
                    log.info("产品 " + msg.getProductId() + " 销量同步完成，+" + delta + "，当前总销量 " + product.getSales());
                }
            }
        } catch (Exception e) {
            log.error("销量同步失败 productId=" + msg.getProductId(), e);
        }
    }
}