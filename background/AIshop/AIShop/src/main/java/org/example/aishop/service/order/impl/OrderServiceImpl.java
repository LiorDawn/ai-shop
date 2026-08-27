package org.example.aishop.service.order.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.dto.OrderDTO;
import org.example.aishop.dto.OrderDetailDTO;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.entity.coupon.Coupon;
import org.example.aishop.entity.coupon.UserCoupon;
import org.example.aishop.entity.order.Cart;
import org.example.aishop.entity.order.OrderItem;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.product.Product;
import org.example.aishop.entity.product.ProductSku;
import org.example.aishop.entity.user.Address;
import org.example.aishop.entity.user.User;
import org.example.aishop.mapper.coupon.CouponMapper;
import org.example.aishop.mapper.coupon.UserCouponMapper;
import org.example.aishop.mapper.merchant.ShopMapper;
import org.example.aishop.mapper.order.AfterSaleMapper;
import org.example.aishop.mapper.order.CartMapper;
import org.example.aishop.mapper.order.OrderItemMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.CommentMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mapper.product.ProductSkuMapper;
import org.example.aishop.mapper.user.AddressMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.mq.message.OrderTimeoutMQMessage;
import org.example.aishop.mq.message.SalesSyncMQMessage;
import org.example.aishop.mq.producer.OrderMqProducer;
import org.example.aishop.service.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private AfterSaleMapper afterSaleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private OrderMqProducer orderMqProducer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("deductStockScript")
    private RedisScript<Long> deductStockScript;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Override
    public Page<OrderDTO> pageOrders(Integer current, Integer size,
                                     String orderNo, Integer orderStatus,
                                     Long shopId, Date startTime, Date endTime) {
        Page<OrderDTO> page = new Page<>(current, size);
        return orderMapper.selectOrderPage(page, orderNo, orderStatus, startTime, endTime, null, shopId);
    }

    @Override
    public OrderDetailDTO getOrderDetail(Long orderId) {
        // 1. 一次 JOIN 查询：订单 + 用户 + 地址
        OrderDetailDTO dto = orderMapper.selectOrderDetail(orderId);
        if (dto == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 2. 一次 JOIN 查询：订单明细 + 商品 + SKU + 店铺 + 售后 + 评论
        List<OrderDetailDTO.OrderItemDTO> items = orderMapper.selectOrderItems(orderId);
        dto.setItems(items);

        return dto;
    }

    @Override
    public void deliverOrder(Long orderId, String logistics) {
        Orders order = super.getById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getOrderStatus() != 1) {
            throw new BusinessException(400, "仅待发货订单可发货");
        }
        order.setOrderStatus(2); // 待收货
        order.setLogistics(logistics);
        super.updateById(order);
    }

    @Override
    public void cancelOrder(Long orderId) {
        Orders order = super.getById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getOrderStatus() != 0) {
            throw new BusinessException(400, "仅待付款订单可取消");
        }
        order.setOrderStatus(4); // 已取消
        super.updateById(order);
    }

    @Override
    public void completeOrder(Long orderId) {
        Orders order = super.getById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getOrderStatus() != 2) {
            throw new BusinessException(400, "仅待收货订单可完成");
        }
        order.setOrderStatus(3); // 已完成
        super.updateById(order);
    }

    @Override
    public Page<OrderDTO> pageMyOrders(Long userId, Integer current, Integer size,
                                       String orderNo, Integer orderStatus) {
        Page<OrderDTO> page = new Page<>(current, size);
        return orderMapper.selectOrderPage(page, orderNo, orderStatus, null, null, userId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, Long addressId, String productIdsStr, String remark, Long couponId) {
        // ==================== 前置校验 ====================

        // 1. 接口限流：每秒最多50次下单
        String rateKey = RedisConstant.orderRateLimitKey(userId);
        Long count = stringRedisTemplate.opsForValue().increment(rateKey);
        if (count == 1) {
            stringRedisTemplate.expire(rateKey, RedisConstant.ORDER_RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        }
        if (count != null && count > RedisConstant.ORDER_RATE_LIMIT_MAX) {
            throw new BusinessException(429, "下单过于频繁，请稍后再试");
        }

        // 2. 校验用户状态
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(400, "账号已被禁用，无法下单");
        }

        // 3. 校验收货地址
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(400, "收货地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(400, "收货地址不属于当前用户");
        }

        // 3.5 校验优惠券
        BigDecimal couponDiscount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        UserCoupon appliedUserCoupon = null;
        if (couponId != null && couponId > 0) {
            appliedUserCoupon = userCouponMapper.selectOne(
                    new LambdaQueryWrapper<UserCoupon>()
                            .eq(UserCoupon::getUserId, userId)
                            .eq(UserCoupon::getCouponId, couponId)
                            .eq(UserCoupon::getStatus, 0));
            if (appliedUserCoupon == null) {
                throw new BusinessException(400, "优惠券不存在或已使用");
            }
            appliedCoupon = couponMapper.selectById(couponId);
            if (appliedCoupon == null || appliedCoupon.getStatus() != 1) {
                throw new BusinessException(400, "优惠券已失效");
            }
            if (appliedCoupon.getEndTime() != null && appliedCoupon.getEndTime().before(new Date())) {
                throw new BusinessException(400, "优惠券已过期");
            }
        }

        // 4. 解析商品ID列表
        List<Long> productIdList = new ArrayList<>();
        if (productIdsStr != null && !productIdsStr.trim().isEmpty()) {
            for (String idStr : productIdsStr.split(",")) {
                try {
                    productIdList.add(Long.parseLong(idStr.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (productIdList.isEmpty()) {
            throw new BusinessException(400, "请先选择要购买的商品");
        }

        // 5. 根据用户 + 商品ID列表查询购物车（Redis优先，MySQL兜底）
        String cartKey = RedisConstant.cartUserKey(userId);
        List<Cart> cartItems = new ArrayList<>();
        boolean fromRedis = false;

        Map<Object, Object> redisEntries = stringRedisTemplate.opsForHash().entries(cartKey);
        if (redisEntries != null && !redisEntries.isEmpty()) {
            for (Long productId : productIdList) {
                String field = String.valueOf(productId);
                String json = (String) redisEntries.get(field);
                if (json == null) continue;
                try {
                    CartItemInRedis cache = objectMapper.readValue(json, CartItemInRedis.class);
                    Cart c = new Cart();
                    c.setUserId(userId);
                    c.setProductId(cache.productId);
                    c.setSkuId(cache.skuId);
                    c.setNum(cache.num != null ? cache.num : 1);
                    c.setChecked(cache.checked != null ? cache.checked : 1);
                    cartItems.add(c);
                } catch (Exception ignored) {}
            }
            fromRedis = !cartItems.isEmpty();
        }

        if (!fromRedis || cartItems.isEmpty()) {
            LambdaQueryWrapper<Cart> mysqlWrapper = new LambdaQueryWrapper<>();
            mysqlWrapper.eq(Cart::getUserId, userId).in(Cart::getProductId, productIdList);
            cartItems = cartMapper.selectList(mysqlWrapper);
        }

        if (cartItems.isEmpty()) {
            throw new BusinessException(400, "请先选择要购买的商品");
        }

        // 6. 批量查商品信息并校验
        Set<Long> productIds = cartItems.stream().map(Cart::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        for (Cart cart : cartItems) {
            Product product = productMap.get(cart.getProductId());
            if (product == null || product.getStatus() == 0) {
                throw new BusinessException(400, "商品已下架");
            }
        }

        // 7. 按店铺分组
        Map<Long, List<Cart>> shopGroupMap = new HashMap<>();
        for (Cart cart : cartItems) {
            Product product = productMap.get(cart.getProductId());
            if (product != null) {
                shopGroupMap.computeIfAbsent(product.getShopId(), k -> new ArrayList<>()).add(cart);
            }
        }

        // ==================== 库存预扣（Redis Lua 原子操作） ====================
        for (Map.Entry<Long, List<Cart>> entry : shopGroupMap.entrySet()) {
            for (Cart cart : entry.getValue()) {
                // 解析 SKU ID：购物车可能未存 skuId，查默认 SKU
                Long skuId = cart.getSkuId();
                if (skuId == null) {
                    LambdaQueryWrapper<ProductSku> skuQuery = new LambdaQueryWrapper<>();
                    skuQuery.eq(ProductSku::getProductId, cart.getProductId()).last("LIMIT 1");
                    ProductSku defaultSku = productSkuMapper.selectOne(skuQuery);
                    if (defaultSku == null) {
                        throw new BusinessException(400, "商品SKU信息不完整");
                    }
                    skuId = defaultSku.getId();
                    cart.setSkuId(skuId); // 回填，后续同步逻辑复用
                }
                String skuKey = RedisConstant.stockSkuKey(skuId);
                String couponKey = "";
                int useCoupon = 0;

                // Redis 中没有库存缓存时，从 MySQL 加载
                Boolean keyExists = stringRedisTemplate.hasKey(skuKey);
                if (keyExists != null && !keyExists) {
                    ProductSku sku = productSkuMapper.selectById(skuId);
                    Integer stock = (sku != null && sku.getStock() != null) ? sku.getStock() : 0;
                    stringRedisTemplate.opsForValue().set(skuKey, String.valueOf(stock));
                }

                Long result = stringRedisTemplate.execute(deductStockScript,
                        Arrays.asList(skuKey, couponKey),
                        String.valueOf(cart.getNum()), String.valueOf(useCoupon));

                if (result == null || result == -1) {
                    throw new BusinessException(400, "库存不足，下单失败");
                }
                if (result == -2) {
                    throw new BusinessException(400, "优惠券已领完");
                }
            }
        }

        // ==================== 同步创建订单（事务内） ====================
        try {
            List<Long> orderIds = new ArrayList<>();
            for (Map.Entry<Long, List<Cart>> entry : shopGroupMap.entrySet()) {
                Long shopId = entry.getKey();
                List<Cart> items = entry.getValue();

                // 计算金额（优先使用 SKU 价格）
                Map<Long, ProductSku> skuMap = new HashMap<>();
                for (Cart cart : items) {
                    if (cart.getSkuId() != null && !skuMap.containsKey(cart.getSkuId())) {
                        ProductSku sku = productSkuMapper.selectById(cart.getSkuId());
                        if (sku != null) skuMap.put(cart.getSkuId(), sku);
                    }
                }
                BigDecimal totalPrice = BigDecimal.ZERO;
                for (Cart cart : items) {
                    Product product = productMap.get(cart.getProductId());
                    BigDecimal unitPrice = product.getPrice();
                    if (cart.getSkuId() != null && skuMap.containsKey(cart.getSkuId())) {
                        ProductSku sku = skuMap.get(cart.getSkuId());
                        if (sku.getPrice() != null) unitPrice = sku.getPrice();
                    }
                    totalPrice = totalPrice.add(unitPrice.multiply(BigDecimal.valueOf(cart.getNum())));
                }

                // 应用优惠券折扣（仅对第一个店铺订单应用，跨店场景暂不支持优惠券）
                BigDecimal couponPrice = BigDecimal.ZERO;
                BigDecimal actualPrice = totalPrice;
                if (appliedCoupon != null && orderIds.isEmpty()) {
                    if (totalPrice.compareTo(appliedCoupon.getMinPrice()) < 0) {
                        throw new BusinessException(400, "未达到优惠券最低使用金额");
                    }
                    if (appliedCoupon.getType() == 1) {
                        // 满减券：直接减
                        couponPrice = appliedCoupon.getDiscount();
                    } else if (appliedCoupon.getType() == 2) {
                        // 折扣券：按折扣计算
                        couponPrice = totalPrice.multiply(BigDecimal.ONE.subtract(appliedCoupon.getDiscount()));
                    }
                    actualPrice = totalPrice.subtract(couponPrice);
                    if (actualPrice.compareTo(BigDecimal.ZERO) < 0) {
                        actualPrice = BigDecimal.ZERO;
                    }
                }

                // 生成订单号（更安全的方式：毫秒时间戳 + 自旋随机后缀）
                long orderId;
                String orderNo;
                int retries = 0;
                do {
                    long ts = System.currentTimeMillis();
                    long rand = (long) (Math.random() * 900) + 100;
                    orderId = ts * 1000 + rand;
                    orderNo = "OD" + ts + rand;
                    retries++;
                } while (orderMapper.selectById(orderId) != null && retries < 10);

                // 1. 写入订单主表
                Orders order = new Orders();
                order.setId(orderId);
                order.setOrderNo(orderNo);
                order.setUserId(userId);
                order.setTotalPrice(totalPrice);
                order.setCouponPrice(couponPrice);
                order.setActualPrice(actualPrice);
                order.setPayType(0);
                order.setPayStatus(0);
                order.setOrderStatus(0);
                order.setAddressId(addressId);
                order.setRemark(remark);
                orderMapper.insert(order);

                // 2. 写入订单明细 + 扣减 MySQL 库存
                StringBuilder stockSnapshot = new StringBuilder("[");
                for (int i = 0; i < items.size(); i++) {
                    Cart cart = items.get(i);
                    Product product = productMap.get(cart.getProductId());
                    Long skuId = cart.getSkuId() != null ? cart.getSkuId() : cart.getProductId();

                    OrderItem item = new OrderItem();
                    item.setOrderId(orderId);
                    item.setProductId(cart.getProductId());
                    item.setSkuId(skuId);
                    item.setShopId(shopId);
                    item.setNum(cart.getNum());
                    // 使用 SKU 价格（与订单总价计算一致）
                    BigDecimal itemPrice = product.getPrice();
                    if (cart.getSkuId() != null && skuMap.containsKey(cart.getSkuId())) {
                        ProductSku itemSku = skuMap.get(cart.getSkuId());
                        if (itemSku.getPrice() != null) itemPrice = itemSku.getPrice();
                    }
                    item.setPrice(itemPrice);
                    orderItemMapper.insert(item);

                    // 扣减 MySQL SKU 库存
                    ProductSku sku = productSkuMapper.selectById(skuId);
                    if (sku != null) {
                        if (sku.getStock() < cart.getNum()) {
                            throw new BusinessException(400, "商品库存不足");
                        }
                        sku.setStock(sku.getStock() - cart.getNum());
                        productSkuMapper.updateById(sku);
                    }

                    if (i > 0) stockSnapshot.append(",");
                    stockSnapshot.append("{\"skuId\":").append(skuId)
                            .append(",\"num\":").append(cart.getNum()).append("}");
                }
                stockSnapshot.append("]");

                // 3. 清空购物车（Redis + MySQL）
                Object[] redisFields = productIdList.stream().map(String::valueOf).toArray();
                stringRedisTemplate.opsForHash().delete(cartKey, redisFields);

                LambdaQueryWrapper<Cart> deleteWrapper = new LambdaQueryWrapper<>();
                deleteWrapper.eq(Cart::getUserId, userId).in(Cart::getProductId, productIdList);
                cartMapper.delete(deleteWrapper);

                // 4. 标记优惠券已使用
                if (appliedUserCoupon != null && orderIds.isEmpty()) {
                    appliedUserCoupon.setStatus(1);
                    userCouponMapper.updateById(appliedUserCoupon);
                }

                // 5. 发送 MQ 异步消息：延时关单（30分钟超时关闭）
                OrderTimeoutMQMessage timeoutMsg = new OrderTimeoutMQMessage();
                timeoutMsg.setOrderId(orderId);
                timeoutMsg.setUserId(userId);
                timeoutMsg.setStockSnapshot(stockSnapshot.toString());
                timeoutMsg.setCouponId(appliedCoupon != null ? appliedCoupon.getId() : null);
                orderMqProducer.sendOrderDelayClose(timeoutMsg);

                orderIds.add(orderId);
            }

            // 6. 清除所有涉及店铺的统计数据缓存，确保商家中心数据实时更新
            for (Long shopId : shopGroupMap.keySet()) {
                try {
                    stringRedisTemplate.delete(RedisConstant.statsMerchantOverviewKey(shopId));
                    stringRedisTemplate.delete(RedisConstant.statsSalesRankingKey(shopId));
                    stringRedisTemplate.delete(RedisConstant.statsOrderTrendKey(shopId));
                } catch (Exception ignored) {}
            }

            return orderIds.get(0);

        } catch (Exception e) {
            // 事务回滚，Redis 库存补偿恢复
            for (Map.Entry<Long, List<Cart>> entry : shopGroupMap.entrySet()) {
                for (Cart cart : entry.getValue()) {
                    try {
                        Long skuId = cart.getSkuId() != null ? cart.getSkuId() : cart.getProductId();
                        String skuKey = RedisConstant.stockSkuKey(skuId);
                        stringRedisTemplate.opsForValue().increment(skuKey, cart.getNum());
                    } catch (Exception ignored) {}
                }
            }
            throw e;
        }
    }

    /** 用于解析 Redis 中购物车 JSON 的辅助类（与 CartServiceImpl.CartItemCache 结构一致） */
    private static class CartItemInRedis {
        public Long productId;
        public Long skuId;
        public Integer num;
        public Integer checked;
        public Long createTime;
    }

    @Override
    public void confirmReceive(Long orderId, Long userId) {
        Orders order = super.getById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此订单");
        }
        if (order.getOrderStatus() != 2) {
            throw new BusinessException(400, "仅待收货订单可确认收货");
        }
        order.setOrderStatus(3);
        super.updateById(order);

        // 订单完成后，异步统计销量 + 增量写入 Redis
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        for (OrderItem item : items) {
            // 增量销量计数
            String salesKey = "AISHOP:SALES:DELTA:" + item.getProductId();
            stringRedisTemplate.opsForValue().increment(salesKey, item.getNum());
            // 发送销量同步 MQ
            SalesSyncMQMessage syncMsg = new SalesSyncMQMessage();
            syncMsg.setProductId(item.getProductId());
            syncMsg.setDelta(item.getNum());
            orderMqProducer.sendSalesSync(syncMsg);
            // 更新热度排行榜 ZSet（销量权重 70%）
            stringRedisTemplate.opsForZSet().incrementScore(
                    RedisConstant.PRODUCT_HOT_RANK_KEY,
                    String.valueOf(item.getProductId()),
                    item.getNum() * 0.7);
        }
    }
}