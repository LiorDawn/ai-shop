package org.example.aishop.service.coupon.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aishop.entity.coupon.FlashSale;
import org.example.aishop.entity.product.Product;
import org.example.aishop.common.result.Result;
import org.example.aishop.mapper.coupon.FlashSaleMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mapper.product.ProductSkuMapper;
import org.example.aishop.mq.message.FlashSaleMQMessage;
import org.example.aishop.service.coupon.FlashSaleService;
import org.example.aishop.util.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.aishop.mq.producer.FlashSaleMqProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FlashSaleServiceImpl extends ServiceImpl<FlashSaleMapper, FlashSale> implements FlashSaleService {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleServiceImpl.class);

    private static final String SECKILL_STOCK_PREFIX = "AISHOP:SECKILL:STOCK:";
    private static final String SECKILL_USER_PREFIX = "AISHOP:SECKILL:USER:";
    private static final String SECKILL_RESULT_PREFIX = "AISHOP:SECKILL:RESULT:";
    private static final String SECKILL_URL_PREFIX = "AISHOP:SECKILL:URL:";

    // 令牌桶：每秒 1000 令牌，桶容量 2000
    private static final long TOKEN_CAPACITY = 2000;
    private static final long TOKEN_RATE = 1000;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RateLimiterService rateLimiterService;
    @Autowired
    private FlashSaleMqProducer flashSaleMqProducer;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper productSkuMapper;
    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Result<?> listActive() {
        LocalDateTime now = LocalDateTime.now();
        List<FlashSale> list = list(new LambdaQueryWrapper<FlashSale>()
                .le(FlashSale::getStartTime, now)
                .ge(FlashSale::getEndTime, now)
                .eq(FlashSale::getStatus, 1)
                .orderByAsc(FlashSale::getStartTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (FlashSale fs : list) {
            Product product = productMapper.selectById(fs.getProductId());
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", fs.getId());
            vo.put("productId", fs.getProductId());
            vo.put("productName", product != null ? product.getName() : "");
            vo.put("productImage", product != null ? product.getImage() : "");
            vo.put("flashPrice", fs.getFlashPrice());
            vo.put("originalPrice", product != null ? product.getPrice() : null);
            vo.put("stock", getRedisStock(fs.getId()));
            vo.put("startTime", fs.getStartTime());
            vo.put("endTime", fs.getEndTime());
            vo.put("limitPerUser", fs.getLimitPerUser());
            result.add(vo);
        }
        return Result.success("查询成功", result);
    }

    @Override
    public Result<?> detail(Long flashSaleId) {
        FlashSale fs = getById(flashSaleId);
        if (fs == null) return Result.fail("秒杀活动不存在");

        Product product = productMapper.selectById(fs.getProductId());
        int stock = getRedisStock(flashSaleId);

        Map<String, Object> vo = new HashMap<>();
        vo.put("id", fs.getId());
        vo.put("productId", fs.getProductId());
        vo.put("productName", product != null ? product.getName() : "");
        vo.put("productImage", product != null ? product.getImage() : "");
        vo.put("flashPrice", fs.getFlashPrice());
        vo.put("originalPrice", product != null ? product.getPrice() : null);
        vo.put("stock", stock);
        vo.put("startTime", fs.getStartTime());
        vo.put("endTime", fs.getEndTime());
        vo.put("limitPerUser", fs.getLimitPerUser());
        vo.put("status", fs.getStatus());
        return Result.success("查询成功", vo);
    }

    @Override
    public Result<?> getSeckillUrl(Long flashSaleId, String verifyCode, String sign) {
        FlashSale fs = getById(flashSaleId);
        if (fs == null) return Result.fail("秒杀活动不存在");

        // 验证签名: MD5(flashSaleId + signKey + verifyCode)
        String expected = DigestUtils.md5DigestAsHex(
                (flashSaleId + fs.getSignKey() + verifyCode).getBytes(StandardCharsets.UTF_8));
        if (!expected.equals(sign)) {
            return Result.fail("非法请求，签名验证失败");
        }

        // 生成一次性秒杀地址（有效期 60 秒）
        String token = UUID.randomUUID().toString().replace("-", "");
        String urlKey = SECKILL_URL_PREFIX + token;
        redisTemplate.opsForValue().set(urlKey, String.valueOf(flashSaleId), 60, TimeUnit.SECONDS);

        Map<String, String> result = new HashMap<>();
        result.put("url", "/flash-sale/execute/" + flashSaleId + "?token=" + token);
        result.put("expireIn", "60");
        return Result.success("获取成功", result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> executeSeckill(Long flashSaleId, String token, Long userId) {
        // 1. 验证 token（前端简化流程：以 "temp-" 开头的临时 token 跳过 Redis 校验）
        if (token == null || token.isEmpty()) {
            return Result.fail("参数错误");
        }
        if (!token.startsWith("temp-")) {
            String urlKey = SECKILL_URL_PREFIX + token;
            String savedId = redisTemplate.opsForValue().get(urlKey);
            if (savedId == null || !savedId.equals(String.valueOf(flashSaleId))) {
                return Result.fail("秒杀地址已失效，请刷新重试");
            }
            redisTemplate.delete(urlKey); // 一次性使用
        }

        // 2. 令牌桶限流
        String tokenBucketKey = "AISHOP:SECKILL:TOKEN:" + flashSaleId;
        if (!rateLimiterService.tryAcquireToken(tokenBucketKey, TOKEN_CAPACITY, TOKEN_RATE, 1)) {
            return Result.fail("当前抢购人数过多，请稍后再试");
        }

        // 3. 一人一单防刷
        String userKey = SECKILL_USER_PREFIX + flashSaleId + ":" + userId;
        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(userKey, "1", 1, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isFirst)) {
            return Result.fail("您已参与过该秒杀活动，请勿重复下单");
        }

        // 4. Redis 预扣库存
        String stockKey = SECKILL_STOCK_PREFIX + flashSaleId;
        Long remain = redisTemplate.opsForValue().decrement(stockKey);
        if (remain == null || remain < 0) {
            redisTemplate.opsForValue().increment(stockKey); // 回滚
            redisTemplate.delete(userKey);
            return Result.fail("商品已售罄");
        }

        // 5. 异步下单
        String requestId = UUID.randomUUID().toString().replace("-", "");
        FlashSale fs = getById(flashSaleId);

        FlashSaleMQMessage msg = new FlashSaleMQMessage();
        msg.setFlashSaleId(flashSaleId);
        msg.setProductId(fs.getProductId());
        msg.setUserId(userId);
        msg.setQuantity(1);
        msg.setOrderNo("FS" + System.currentTimeMillis() + userId);
        msg.setRequestId(requestId);

        flashSaleMqProducer.sendSeckillOrder(msg);

        // 初始化结果状态
        redisTemplate.opsForValue().set(SECKILL_RESULT_PREFIX + requestId, "PENDING", 5, TimeUnit.MINUTES);

        Map<String, String> result = new HashMap<>();
        result.put("requestId", requestId);
        result.put("message", "抢购请求已提交，正在处理中...");
        return Result.success("抢购成功", result);
    }

    @Override
    public Result<?> pollResult(String requestId) {
        String resultKey = SECKILL_RESULT_PREFIX + requestId;
        String status = redisTemplate.opsForValue().get(resultKey);
        if (status == null) {
            return Result.fail("订单不存在或已过期");
        }
        if ("PENDING".equals(status)) {
            return Result.success("处理中", Map.of("status", "PENDING"));
        }
        if ("SUCCESS".equals(status)) {
            String orderId = redisTemplate.opsForValue().get(resultKey + ":orderId");
            return Result.success("下单成功", Map.of("status", "SUCCESS", "orderId", orderId != null ? orderId : ""));
        }
        return Result.fail("下单失败: " + status);
    }

    // ==================== 辅助方法 ====================

    private int getRedisStock(Long flashSaleId) {
        String stockKey = SECKILL_STOCK_PREFIX + flashSaleId;
        String val = redisTemplate.opsForValue().get(stockKey);
        if (val != null) {
            return Integer.parseInt(val);
        }
        // 从数据库加载
        FlashSale fs = getById(flashSaleId);
        if (fs != null) {
            redisTemplate.opsForValue().set(stockKey, String.valueOf(fs.getStock()), 1, TimeUnit.HOURS);
            return fs.getStock();
        }
        return 0;
    }
}