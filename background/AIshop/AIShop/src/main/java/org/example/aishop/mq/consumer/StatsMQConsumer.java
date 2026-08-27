package org.example.aishop.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.entity.coupon.UserCoupon;
import org.example.aishop.entity.merchant.ShopFollow;
import org.example.aishop.entity.order.AfterSale;
import org.example.aishop.entity.order.OrderItem;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.product.Product;
import org.example.aishop.entity.user.User;
import org.example.aishop.mapper.coupon.UserCouponMapper;
import org.example.aishop.mapper.merchant.ShopFollowMapper;
import org.example.aishop.mapper.order.AfterSaleMapper;
import org.example.aishop.mapper.order.OrderItemMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.mq.message.StatsMQMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 统计任务 MQ 消费者
 * 异步计算管理端/商家端统计指标，结果缓存到 Redis
 */
@Component
public class StatsMQConsumer {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ShopFollowMapper shopFollowMapper;
    @Autowired
    private AfterSaleMapper afterSaleMapper;
    @Autowired
    private UserCouponMapper userCouponMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 监听统计任务队列
     */
    @RabbitListener(queues = MQConstant.STATS_QUEUE)
    public void onStatsMessage(StatsMQMessage msg) {
        if (msg == null || msg.getStatsType() == null) return;
        try {
            switch (msg.getStatsType()) {
                case "ADMIN_OVERVIEW":
                    computeAdminOverview();
                    break;
                case "MERCHANT_OVERVIEW":
                    computeMerchantOverview(msg.getShopId());
                    break;
                case "SALES_RANKING":
                    computeSalesRanking(msg.getShopId());
                    break;
                case "ORDER_TREND":
                    computeOrderTrend(msg.getShopId());
                    break;
            }
        } catch (Exception e) {
            // 统计失败不影响主流程
        }
    }

    // ========== 管理端大盘统计 ==========

    private void computeAdminOverview() throws JsonProcessingException {
        Map<String, Object> stats = new HashMap<>();

        // 总销售额：所有已支付订单
        LambdaQueryWrapper<Orders> paidQw = new LambdaQueryWrapper<>();
        paidQw.eq(Orders::getPayStatus, 1);
        List<Orders> paidOrders = orderMapper.selectList(paidQw);
        BigDecimal totalSales = paidOrders.stream()
                .map(o -> o.getActualPrice() != null ? o.getActualPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalSales", totalSales);

        // 总订单量：排除已取消
        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Orders>().ne(Orders::getOrderStatus, 4));
        stats.put("totalOrders", orderCount);

        // 总用户数
        LambdaQueryWrapper<User> userQw = new LambdaQueryWrapper<>();
        userQw.ne(User::getRoleId, 1);
        stats.put("totalUsers", userMapper.selectCount(userQw));

        // 优惠券使用率
        Long totalClaimed = userCouponMapper.selectCount(null);
        if (totalClaimed != null && totalClaimed > 0) {
            LambdaQueryWrapper<UserCoupon> usedQw = new LambdaQueryWrapper<>();
            usedQw.eq(UserCoupon::getStatus, 1);
            Long usedCount = userCouponMapper.selectCount(usedQw);
            double rate = BigDecimal.valueOf(usedCount != null ? usedCount : 0)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalClaimed), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            stats.put("couponUsageRate", String.format("%.2f%%", rate));
        } else {
            stats.put("couponUsageRate", "0.00%");
        }

        // 售后率：按排除已取消的订单数计算
        Long afterSaleCount = afterSaleMapper.selectCount(null);
        long effectiveOrders = orderCount != null ? orderCount : 0;
        if (effectiveOrders > 0 && afterSaleCount != null) {
            double rate = BigDecimal.valueOf(afterSaleCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(effectiveOrders), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            stats.put("afterSaleRate", String.format("%.2f%%", rate));
        } else {
            stats.put("afterSaleRate", "0.00%");
        }

        // 商品销量排行（TOP 10）
        stats.put("productSalesRank", orderItemMapper.selectProductSalesRank());

        stringRedisTemplate.opsForValue().set(RedisConstant.STATS_ADMIN_OVERVIEW_KEY,
                objectMapper.writeValueAsString(stats),
                RedisConstant.STATS_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    // ========== 商家端仪表盘统计 ==========

    private void computeMerchantOverview(Long shopId) throws JsonProcessingException {
        if (shopId == null) return;

        Map<String, Object> data = new HashMap<>();
        LocalDate today = LocalDate.now();

        Set<Long> shopOrderIds = getShopOrderIds(shopId);

        data.put("todayOrderCount", countOrdersInRange(shopOrderIds, today, today));
        data.put("todayRevenue", sumOrderAmountInRange(shopOrderIds, today, today));
        data.put("yesterdayOrderCount", countOrdersInRange(shopOrderIds, today.minusDays(1), today.minusDays(1)));
        data.put("yesterdayRevenue", sumOrderAmountInRange(shopOrderIds, today.minusDays(1), today.minusDays(1)));
        data.put("weekOrderCount", countOrdersInRange(shopOrderIds, today.minusDays(6), today));
        data.put("weekRevenue", sumOrderAmountInRange(shopOrderIds, today.minusDays(6), today));
        data.put("pendingShipCount", countOrdersByStatus(shopOrderIds, 1));
        data.put("pendingAfterSaleCount", countPendingAfterSales(shopId));
        data.put("onSaleProductCount", productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getShopId, shopId).eq(Product::getStatus, 1)));
        data.put("followerCount", shopFollowMapper.selectCount(
                new LambdaQueryWrapper<ShopFollow>().eq(ShopFollow::getShopId, shopId)));

        stringRedisTemplate.opsForValue().set(RedisConstant.statsMerchantOverviewKey(shopId),
                objectMapper.writeValueAsString(data),
                RedisConstant.STATS_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    // ========== 商家端热销排行 ==========

    private void computeSalesRanking(Long shopId) throws JsonProcessingException {
        if (shopId == null) return;

        Set<Long> shopOrderIds = getShopOrderIds(shopId);
        if (shopOrderIds.isEmpty()) {
            stringRedisTemplate.opsForValue().set(RedisConstant.statsSalesRankingKey(shopId),
                    "[]", RedisConstant.STATS_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            return;
        }

        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrderItem::getOrderId, shopOrderIds).eq(OrderItem::getShopId, shopId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);

        Map<Long, List<OrderItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> entry : grouped.entrySet()) {
            Long productId = entry.getKey();
            int totalSold = entry.getValue().stream().mapToInt(OrderItem::getNum).sum();
            BigDecimal totalAmount = entry.getValue().stream()
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getNum())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Product product = productMapper.selectById(productId);
            if (product != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("productId", productId);
                item.put("productName", product.getName());
                item.put("productImage", product.getImage());
                item.put("totalSold", totalSold);
                item.put("totalAmount", totalAmount);
                ranking.add(item);
            }
        }
        ranking.sort((a, b) -> Integer.compare((int) b.get("totalSold"), (int) a.get("totalSold")));
        if (ranking.size() > 20) ranking = ranking.subList(0, 20);

        stringRedisTemplate.opsForValue().set(RedisConstant.statsSalesRankingKey(shopId),
                objectMapper.writeValueAsString(ranking),
                RedisConstant.STATS_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    // ========== 商家端订单趋势 ==========

    private void computeOrderTrend(Long shopId) throws JsonProcessingException {
        if (shopId == null) return;

        Set<Long> shopOrderIds = getShopOrderIds(shopId);
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            point.put("count", countOrdersInRange(shopOrderIds, date, date));
            point.put("revenue", sumOrderAmountInRange(shopOrderIds, date, date));
            trend.add(point);
        }

        stringRedisTemplate.opsForValue().set(RedisConstant.statsOrderTrendKey(shopId),
                objectMapper.writeValueAsString(trend),
                RedisConstant.STATS_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    // ========== 辅助方法 ==========

    private Set<Long> getShopOrderIds(Long shopId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getShopId, shopId);
        return orderItemMapper.selectList(wrapper).stream()
                .map(OrderItem::getOrderId).collect(Collectors.toSet());
    }

    private long countOrdersInRange(Set<Long> orderIds, LocalDate start, LocalDate end) {
        if (orderIds.isEmpty()) return 0;
        String startStr = start.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String endStr = end.atTime(23, 59, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .in(Orders::getId, orderIds)
                .ne(Orders::getOrderStatus, 4) // 排除已取消
                .ge(Orders::getCreateTime, startStr)
                .le(Orders::getCreateTime, endStr));
    }

    private BigDecimal sumOrderAmountInRange(Set<Long> orderIds, LocalDate start, LocalDate end) {
        if (orderIds.isEmpty()) return BigDecimal.ZERO;
        String startStr = start.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String endStr = end.atTime(23, 59, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                .in(Orders::getId, orderIds)
                .ge(Orders::getCreateTime, startStr)
                .le(Orders::getCreateTime, endStr)
                .eq(Orders::getPayStatus, 1)).stream()
                .map(o -> o.getActualPrice() != null ? o.getActualPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countOrdersByStatus(Set<Long> orderIds, Integer status) {
        if (orderIds.isEmpty()) return 0;
        return orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .in(Orders::getId, orderIds).eq(Orders::getOrderStatus, status));
    }

    private long countPendingAfterSales(Long shopId) {
        return afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getShopId, shopId).eq(AfterSale::getAuditStatus, 0));
    }
}