package org.example.aishop.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aishop.entity.coupon.FlashSale;
import org.example.aishop.entity.order.OrderItem;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.product.Product;
import org.example.aishop.entity.product.ProductSku;
import org.example.aishop.entity.user.Address;
import org.example.aishop.mapper.order.OrderItemMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mapper.product.ProductSkuMapper;
import org.example.aishop.mapper.user.AddressMapper;
import org.example.aishop.mq.message.FlashSaleMQMessage;
import org.example.aishop.service.coupon.FlashSaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单异步消费者
 */
@Component
public class FlashSaleConsumer {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleConsumer.class);
    private static final String SECKILL_RESULT_PREFIX = "AISHOP:SECKILL:RESULT:";
    private static final String SECKILL_STOCK_PREFIX = "AISHOP:SECKILL:STOCK:";

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper productSkuMapper;
    @Autowired
    private FlashSaleService flashSaleService;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @RabbitListener(queues = "aishop.seckill.queue")
    @Transactional(rollbackFor = Exception.class)
    public void onFlashSaleOrder(FlashSaleMQMessage msg) {
        String resultKey = SECKILL_RESULT_PREFIX + msg.getRequestId();
        try {
            FlashSale fs = flashSaleService.getById(msg.getFlashSaleId());
            if (fs == null) {
                redisTemplate.opsForValue().set(resultKey, "FAIL:活动不存在", 5, TimeUnit.MINUTES);
                return;
            }

            Product product = productMapper.selectById(msg.getProductId());
            if (product == null) {
                redisTemplate.opsForValue().set(resultKey, "FAIL:商品不存在", 5, TimeUnit.MINUTES);
                return;
            }

            // 查询用户默认收货地址
            Address defaultAddress = addressMapper.selectOne(
                new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, msg.getUserId())
                    .eq(Address::getIsDefault, 1)
            );
            if (defaultAddress == null) {
                redisTemplate.opsForValue().set(resultKey, "FAIL:未设置默认地址", 5, TimeUnit.MINUTES);
                return;
            }

            // 扣减秒杀活动库存（数据库库存由 FlashSale 表管理）
            String stockKey = SECKILL_STOCK_PREFIX + msg.getFlashSaleId();
            fs.setStock(fs.getStock() - msg.getQuantity());
            flashSaleService.updateById(fs);
            redisTemplate.opsForValue().set(stockKey, String.valueOf(fs.getStock()), 1, TimeUnit.HOURS);

            // 创建订单
            Orders order = new Orders();
            order.setOrderNo(msg.getOrderNo());
            order.setUserId(msg.getUserId());
            order.setTotalPrice(fs.getFlashPrice().multiply(new BigDecimal(msg.getQuantity())));
            order.setActualPrice(fs.getFlashPrice().multiply(new BigDecimal(msg.getQuantity()))); // 实付金额 = 秒杀价
            order.setAddressId(defaultAddress.getId());
            order.setOrderStatus(0); // 待支付（与 PayConstant.ORDER_STATUS_PENDING_PAY 一致）
            order.setCreateTime(new Date());
            orderMapper.insert(order);

            // 创建订单明细
            // 查询默认 SKU（消息中未携带 skuId）
            Long skuId = msg.getSkuId();
            if (skuId == null) {
                ProductSku defaultSku = productSkuMapper.selectOne(
                    new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, msg.getProductId())
                        .last("LIMIT 1")
                );
                skuId = defaultSku != null ? defaultSku.getId() : null;
            }

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(msg.getProductId());
            item.setSkuId(skuId);
            item.setShopId(product.getShopId());
            item.setNum(msg.getQuantity() != null ? msg.getQuantity() : 1);
            item.setPrice(fs.getFlashPrice());
            orderItemMapper.insert(item);

            redisTemplate.opsForValue().set(resultKey, "SUCCESS", 5, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(resultKey + ":orderId", String.valueOf(order.getId()), 5, TimeUnit.MINUTES);

            log.info("秒杀订单创建成功: requestId=" + msg.getRequestId() + ", orderId=" + order.getId() + ", userId=" + msg.getUserId() + ", productId=" + msg.getProductId());

        } catch (Exception e) {
            log.error("秒杀订单处理失败: requestId=" + msg.getRequestId() + ", error=" + e.getMessage());
            redisTemplate.opsForValue().set(resultKey, "FAIL:系统异常", 5, TimeUnit.MINUTES);
        }
    }
}