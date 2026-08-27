package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.MQConstant;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.user.User;
import org.example.aishop.entity.coupon.UserCoupon;
import org.example.aishop.common.result.Result;
import org.example.aishop.dto.StatsDTO;
import org.example.aishop.mapper.coupon.UserCouponMapper;
import org.example.aishop.mapper.order.AfterSaleMapper;
import org.example.aishop.mapper.order.OrderItemMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.mq.message.StatsMQMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Tag(name = "管理端数据统计", description = "全平台数据概览、统计")
@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private UserCouponMapper userCouponMapper;
    @Autowired
    private AfterSaleMapper afterSaleMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "数据概览", description = "全平台订单、用户、商家、营收数据，含 Redis 缓存")
    @GetMapping("/summary")
    public Result<StatsDTO> summary() {
        // 尝试从 Redis 缓存读取
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstant.STATS_ADMIN_OVERVIEW_KEY);
            if (cached != null) {
                Map<String, Object> map = objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
                return Result.success("查询成功", mapToStatsDTO(map));
            }
        } catch (Exception ignored) {}

        // 缓存未命中 → 实时计算并返回
        StatsDTO stats = computeRealTime();

        // 触发 MQ 异步刷新缓存
        try {
            rabbitTemplate.convertAndSend(MQConstant.STATS_EXCHANGE,
                    MQConstant.STATS_ROUTING_KEY, StatsMQMessage.adminOverview());
        } catch (Exception ignored) {}

        return Result.success("查询成功", stats);
    }

    private StatsDTO computeRealTime() {
        StatsDTO stats = new StatsDTO();

        // 总销售额：所有已支付订单（payStatus=1）
        LambdaQueryWrapper<Orders> paidQw = new LambdaQueryWrapper<>();
        paidQw.eq(Orders::getPayStatus, 1);
        List<Orders> paidOrders = orderMapper.selectList(paidQw);
        BigDecimal totalSales = paidOrders.stream()
                .map(o -> o.getActualPrice() != null ? o.getActualPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalSales(totalSales);

        // 总订单数：排除已取消（orderStatus=4）
        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<Orders>().ne(Orders::getOrderStatus, 4));
        stats.setTotalOrders(orderCount != null ? orderCount.intValue() : 0);

        LambdaQueryWrapper<User> userQw = new LambdaQueryWrapper<>();
        userQw.ne(User::getRoleId, 1);
        Long userCount = userMapper.selectCount(userQw);
        stats.setTotalUsers(userCount != null ? userCount.intValue() : 0);

        Long totalClaimed = userCouponMapper.selectCount(null);
        if (totalClaimed != null && totalClaimed > 0) {
            LambdaQueryWrapper<UserCoupon> usedQw = new LambdaQueryWrapper<>();
            usedQw.eq(UserCoupon::getStatus, 1);
            Long usedCount = userCouponMapper.selectCount(usedQw);
            double rate = BigDecimal.valueOf(usedCount != null ? usedCount : 0)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalClaimed), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            stats.setCouponUsageRate(String.format("%.2f%%", rate));
        } else {
            stats.setCouponUsageRate("0.00%");
        }

        if (orderCount != null && orderCount > 0) {
            Long afterSaleCount = afterSaleMapper.selectCount(null);
            double rate = BigDecimal.valueOf(afterSaleCount != null ? afterSaleCount : 0)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            stats.setAfterSaleRate(String.format("%.2f%%", rate));
        } else {
            stats.setAfterSaleRate("0.00%");
        }

        List<StatsDTO.ProductSalesRankVO> rankList = orderItemMapper.selectProductSalesRank();
        stats.setProductSalesRank(rankList);

        return stats;
    }

    private StatsDTO mapToStatsDTO(Map<String, Object> map) {
        StatsDTO dto = new StatsDTO();
        if (map.get("totalSales") instanceof Number) {
            dto.setTotalSales(new BigDecimal(map.get("totalSales").toString()));
        }
        if (map.get("totalOrders") instanceof Number) {
            dto.setTotalOrders(((Number) map.get("totalOrders")).intValue());
        }
        if (map.get("totalUsers") instanceof Number) {
            dto.setTotalUsers(((Number) map.get("totalUsers")).intValue());
        }
        dto.setCouponUsageRate((String) map.getOrDefault("couponUsageRate", "0.00%"));
        dto.setAfterSaleRate((String) map.getOrDefault("afterSaleRate", "0.00%"));
        // 商品排行（从缓存反序列化后是 List<Map>，需转成 VO 对象）
        Object rankObj = map.get("productSalesRank");
        if (rankObj instanceof List) {
            try {
                String json = objectMapper.writeValueAsString(rankObj);
                List<StatsDTO.ProductSalesRankVO> rankList = objectMapper.readValue(json,
                        new TypeReference<List<StatsDTO.ProductSalesRankVO>>() {});
                dto.setProductSalesRank(rankList);
            } catch (Exception ignored) {}
        }
        return dto;
    }
}