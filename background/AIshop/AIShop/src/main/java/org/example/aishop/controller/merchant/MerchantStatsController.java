package org.example.aishop.controller.merchant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.common.result.Result;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.entity.merchant.ShopFollow;
import org.example.aishop.entity.order.AfterSale;
import org.example.aishop.entity.order.OrderItem;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.product.Product;
import org.example.aishop.mapper.merchant.ShopFollowMapper;
import org.example.aishop.mapper.order.AfterSaleMapper;
import org.example.aishop.mapper.order.OrderItemMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mq.message.StatsMQMessage;
import org.example.aishop.util.UserHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "商家数据统计", description = "商家仪表盘数据概览、营收统计")
@RestController
@RequestMapping("/merchant/stats")
public class MerchantStatsController {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShopFollowMapper shopFollowMapper;
    @Autowired
    private AfterSaleMapper afterSaleMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "首页仪表盘", description = "商家数据概览，含 Redis 缓存")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Long shopId = getCurrentShopId();

        // 优先从 Redis 缓存读取
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstant.statsMerchantOverviewKey(shopId));
            if (cached != null) {
                Map<String, Object> data = objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
                return Result.success("查询成功", data);
            }
        } catch (Exception ignored) {}

        // 缓存未命中 → 实时计算
        Map<String, Object> data = computeOverviewRealTime(shopId);

        // 触发 MQ 异步刷新
        try {
            rabbitTemplate.convertAndSend(MQConstant.STATS_EXCHANGE,
                    MQConstant.STATS_ROUTING_KEY, StatsMQMessage.merchantOverview(shopId));
        } catch (Exception ignored) {}

        return Result.success("查询成功", data);
    }

    /** 热销商品排行 */
    @GetMapping("/sales-ranking")
    public Result<List<Map<String, Object>>> salesRanking() {
        Long shopId = getCurrentShopId();

        // 优先从 Redis 缓存读取
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstant.statsSalesRankingKey(shopId));
            if (cached != null) {
                List<Map<String, Object>> ranking = objectMapper.readValue(cached,
                        new TypeReference<List<Map<String, Object>>>() {});
                return Result.success("查询成功", ranking);
            }
        } catch (Exception ignored) {}

        // 缓存未命中 → 实时计算
        List<Map<String, Object>> ranking = computeSalesRankingRealTime(shopId);

        // 触发 MQ 异步刷新
        try {
            rabbitTemplate.convertAndSend(MQConstant.STATS_EXCHANGE,
                    MQConstant.STATS_ROUTING_KEY, StatsMQMessage.salesRanking(shopId));
        } catch (Exception ignored) {}

        return Result.success("查询成功", ranking);
    }

    @Operation(summary = "订单趋势", description = "近7天订单数量和金额趋势")
    @GetMapping("/order-trend")
    public Result<List<Map<String, Object>>> orderTrend() {
        Long shopId = getCurrentShopId();

        // 优先从 Redis 缓存读取
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstant.statsOrderTrendKey(shopId));
            if (cached != null) {
                List<Map<String, Object>> trend = objectMapper.readValue(cached,
                        new TypeReference<List<Map<String, Object>>>() {});
                return Result.success("查询成功", trend);
            }
        } catch (Exception ignored) {}

        // 缓存未命中 → 实时计算
        List<Map<String, Object>> trend = computeOrderTrendRealTime(shopId);

        // 触发 MQ 异步刷新
        try {
            rabbitTemplate.convertAndSend(MQConstant.STATS_EXCHANGE,
                    MQConstant.STATS_ROUTING_KEY, StatsMQMessage.orderTrend(shopId));
        } catch (Exception ignored) {}

        return Result.success("查询成功", trend);
    }

    // ===== 实时计算方法（缓存未命中时的兜底逻辑） =====

    private Map<String, Object> computeOverviewRealTime(Long shopId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        String todayEnd = today.atTime(23, 59, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String weekAgo = today.minusDays(6).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Set<Long> shopOrderIds = getShopOrderIds(shopId);

        Map<String, Object> data = new HashMap<>();
        data.put("todayOrderCount", countOrders(shopOrderIds, today, today));
        data.put("todayRevenue", sumOrderAmount(shopOrderIds, today, today));
        data.put("yesterdayOrderCount", countOrders(shopOrderIds, yesterday, yesterday));
        data.put("yesterdayRevenue", sumOrderAmount(shopOrderIds, yesterday, yesterday));
        data.put("weekOrderCount", countOrders(shopOrderIds, today.minusDays(6), today));
        data.put("weekRevenue", sumOrderAmount(shopOrderIds, today.minusDays(6), today));
        data.put("pendingShipCount", countOrdersByStatus(shopOrderIds, 1));
        data.put("pendingAfterSaleCount", countPendingAfterSales(shopId));
        data.put("onSaleProductCount", productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getShopId, shopId).eq(Product::getStatus, 1)));
        data.put("followerCount", shopFollowMapper.selectCount(
                new LambdaQueryWrapper<ShopFollow>().eq(ShopFollow::getShopId, shopId)));
        return data;
    }

    private List<Map<String, Object>> computeSalesRankingRealTime(Long shopId) {
        Set<Long> shopOrderIds = getShopOrderIds(shopId);
        if (shopOrderIds.isEmpty()) return Collections.emptyList();

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
                    .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

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
        return ranking;
    }

    private List<Map<String, Object>> computeOrderTrendRealTime(Long shopId) {
        Set<Long> shopOrderIds = getShopOrderIds(shopId);
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            point.put("count", countOrders(shopOrderIds, date, date));
            point.put("revenue", sumOrderAmount(shopOrderIds, date, date));
            trend.add(point);
        }
        return trend;
    }

    // ===== 辅助方法 =====

    private Long getCurrentShopId() {
        Long shopId = UserHolder.getShopId();
        if (shopId == null) throw new BusinessException(401, "商家未登录或未绑定店铺");
        return shopId;
    }

    private Set<Long> getShopOrderIds(Long shopId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getShopId, shopId))
                .stream().map(OrderItem::getOrderId).collect(Collectors.toSet());
    }

    private long countOrders(Set<Long> orderIds, LocalDate start, LocalDate end) {
        if (orderIds.isEmpty()) return 0;
        return orderMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .in(Orders::getId, orderIds)
                .ne(Orders::getOrderStatus, 4) // 排除已取消
                .ge(Orders::getCreateTime, start.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .le(Orders::getCreateTime, end.atTime(23, 59, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
    }

    private BigDecimal sumOrderAmount(Set<Long> orderIds, LocalDate start, LocalDate end) {
        if (orderIds.isEmpty()) return BigDecimal.ZERO;
        return orderMapper.selectList(new LambdaQueryWrapper<Orders>()
                .in(Orders::getId, orderIds)
                .ge(Orders::getCreateTime, start.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .le(Orders::getCreateTime, end.atTime(23, 59, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .eq(Orders::getPayStatus, 1)).stream()
                .map(o -> o.getActualPrice() != null ? o.getActualPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
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