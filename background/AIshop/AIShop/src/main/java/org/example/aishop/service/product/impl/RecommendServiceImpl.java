package org.example.aishop.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.entity.order.OrderItem;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.entity.product.Product;
import org.example.aishop.dto.ProductDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.mapper.order.OrderItemMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.service.product.ProductService;
import org.example.aishop.service.product.RecommendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);

    private static final String RECOMMEND_GUESS_PREFIX = "AISHOP:RECOMMEND:GUESS:";
    private static final String RECOMMEND_VIEWED_PREFIX = "AISHOP:RECOMMEND:VIEWED:";
    private static final String RECOMMEND_BOUGHT_PREFIX = "AISHOP:RECOMMEND:BOUGHT:";
    private static final String RECOMMEND_SIMILAR_PREFIX = "AISHOP:RECOMMEND:SIMILAR:";
    private static final long RECOMMEND_TTL = 30 * 60; // 30分钟

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public Result<List<ProductDTO>> guessYouLike(Long userId, int limit) {
        // 先查缓存
        String cacheKey = RECOMMEND_GUESS_PREFIX + userId;
        List<ProductDTO> cached = getCachedProducts(cacheKey);
        if (cached != null) return Result.success("猜你喜欢", cached);

        List<ProductDTO> result = new ArrayList<>();
        Set<Long> addedIds = new HashSet<>();

        // 1. 基于协同过滤：找相似用户喜欢的商品
        List<Long> cfProducts = collaborativeFilter(userId, limit);
        for (Long pid : cfProducts) {
            if (addedIds.size() >= limit) break;
            if (addedIds.add(pid)) {
                ProductDTO dto = productService.getProductById(pid);
                if (dto != null) result.add(dto);
            }
        }

        // 2. 补充热门商品
        if (result.size() < limit) {
            List<Long> hotIds = productService.getHotProductIds(limit);
            for (Long pid : hotIds) {
                if (result.size() >= limit) break;
                if (addedIds.add(pid)) {
                    ProductDTO dto = productService.getProductById(pid);
                    if (dto != null) result.add(dto);
                }
            }
        }

        cacheProducts(cacheKey, result);
        return Result.success("猜你喜欢", result);
    }

    @Override
    public Result<List<ProductDTO>> alsoViewed(Long productId, int limit) {
        String cacheKey = RECOMMEND_VIEWED_PREFIX + productId;
        List<ProductDTO> cached = getCachedProducts(cacheKey);
        if (cached != null) return Result.success("看了又看", cached);

        // 找到浏览过该商品的用户
        String viewKey = RedisConstant.PRODUCT_VIEW_PREFIX + productId;
        // 浏览过同一分类的其他商品
        Product product = productMapper.selectById(productId);
        if (product == null || product.getCategoryId() == null) {
            return Result.success("看了又看", Collections.emptyList());
        }

        List<Product> sameCategory = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getCategoryId, product.getCategoryId())
                        .ne(Product::getId, productId)
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT " + limit));

        List<ProductDTO> result = sameCategory.stream()
                .map(p -> {
                    ProductDTO dto = new ProductDTO();
                    dto.setId(p.getId());
                    dto.setName(p.getName());
                    dto.setPrice(p.getPrice());
                    dto.setImage(p.getImage());
                    dto.setSales(p.getSales());
                    return dto;
                }).collect(Collectors.toList());

        cacheProducts(cacheKey, result);
        return Result.success("看了又看", result);
    }

    @Override
    public Result<List<ProductDTO>> alsoBought(Long productId, int limit) {
        String cacheKey = RECOMMEND_BOUGHT_PREFIX + productId;
        List<ProductDTO> cached = getCachedProducts(cacheKey);
        if (cached != null) return Result.success("买了又买", cached);

        // 找到购买了该商品的所有订单
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getProductId, productId));
        if (items.isEmpty()) {
            return Result.success("买了又买", Collections.emptyList());
        }

        Set<Long> orderIds = items.stream().map(OrderItem::getOrderId).collect(Collectors.toSet());
        // 找到这些订单中的其他商品
        List<OrderItem> otherItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
                        .ne(OrderItem::getProductId, productId));
        if (otherItems.isEmpty()) {
            return Result.success("买了又买", Collections.emptyList());
        }

        // 统计商品出现次数
        Map<Long, Long> productCount = otherItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getProductId, Collectors.counting()));
        List<Long> topProducts = productCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<ProductDTO> result = topProducts.stream()
                .map(productService::getProductById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        cacheProducts(cacheKey, result);
        return Result.success("买了又买", result);
    }

    @Override
    public Result<List<ProductDTO>> similarProducts(Long productId, int limit) {
        String cacheKey = RECOMMEND_SIMILAR_PREFIX + productId;
        List<ProductDTO> cached = getCachedProducts(cacheKey);
        if (cached != null) return Result.success("同类商品", cached);

        Product product = productMapper.selectById(productId);
        if (product == null || product.getCategoryId() == null) {
            return Result.success("同类商品", Collections.emptyList());
        }

        List<Product> similar = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getCategoryId, product.getCategoryId())
                        .ne(Product::getId, productId)
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT " + limit));

        List<ProductDTO> result = similar.stream()
                .map(p -> {
                    ProductDTO dto = new ProductDTO();
                    dto.setId(p.getId());
                    dto.setName(p.getName());
                    dto.setPrice(p.getPrice());
                    dto.setImage(p.getImage());
                    dto.setSales(p.getSales());
                    return dto;
                }).collect(Collectors.toList());

        cacheProducts(cacheKey, result);
        return Result.success("同类商品", result);
    }

    // ==================== 协同过滤 ====================

    private List<Long> collaborativeFilter(Long userId, int limit) {
        // 1. 获取用户的行为数据：浏览、收藏、购买的商品
        Set<Long> userProducts = new HashSet<>();

        // 浏览过的商品（使用独立 key 前缀，避免与 PRODUCT_VIEW 计数器冲突）
        String userViewKey = "AISHOP:USER:VIEW:" + userId;
        try {
            Set<String> viewed = redisTemplate.opsForZSet().reverseRange(userViewKey, 0, 50);
            if (viewed != null) {
                userProducts.addAll(viewed.stream().map(Long::parseLong).collect(Collectors.toSet()));
            }
        } catch (Exception e) {
            log.debug("用户浏览记录读取失败（key 不存在或类型冲突）: {}", e.getMessage());
        }

        // 收藏的商品
        String collectKey = RedisConstant.COLLECT_USER_PREFIX + userId;
        try {
            Set<String> collected = redisTemplate.opsForSet().members(collectKey);
            if (collected != null) {
                userProducts.addAll(collected.stream().map(Long::parseLong).collect(Collectors.toSet()));
            }
        } catch (Exception e) {
            log.debug("用户收藏记录读取失败（key 不存在或类型冲突）: {}", e.getMessage());
        }

        // 购买过的商品
        List<Orders> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId));
        for (Orders order : orders) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            for (OrderItem item : items) {
                userProducts.add(item.getProductId());
            }
        }

        if (userProducts.isEmpty()) return Collections.emptyList();

        // 2. 基于商品类别协同：找到同类热门商品
        List<Product> userProductList = productMapper.selectBatchIds(userProducts);
        Set<Long> categoryIds = userProductList.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) return Collections.emptyList();

        // 3. 推荐同类热门商品（排除用户已交互的）
        return productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .in(Product::getCategoryId, categoryIds)
                        .notIn(Product::getId, userProducts)
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT " + limit))
                .stream()
                .map(Product::getId)
                .collect(Collectors.toList());
    }

    // ==================== 缓存（JSON序列化） ====================

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private List<ProductDTO> getCachedProducts(String key) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null || cached.isEmpty() || "[]".equals(cached)) return null;
        try {
            return objectMapper.readValue(cached,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ProductDTO.class));
        } catch (JsonProcessingException e) {
            log.warn("推荐缓存反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private void cacheProducts(String key, List<ProductDTO> products) {
        try {
            String json = objectMapper.writeValueAsString(products);
            redisTemplate.opsForValue().set(key, json, RECOMMEND_TTL, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("推荐缓存序列化失败: {}", e.getMessage());
        }
    }

    /**
     * 定时离线计算热门推荐缓存（每 30 分钟）
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void refreshRecommendCache() {
        log.info("开始刷新推荐缓存...");
        // 清理所有推荐缓存，下次访问时重新计算
        Set<String> keys = redisTemplate.keys("AISHOP:RECOMMEND:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("推荐缓存刷新完成，清理了 " + (keys != null ? keys.size() : 0) + " 个缓存 key");
    }
}