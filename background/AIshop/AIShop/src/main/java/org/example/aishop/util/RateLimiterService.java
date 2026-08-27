package org.example.aishop.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

/**
 * 秒杀令牌桶限流
 * 基于 Redis Lua 脚本保证原子性
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== 令牌桶 ====================

    private static final String TOKEN_BUCKET_LUA =
        "local key = KEYS[1] " +
        "local capacity = tonumber(ARGV[1]) " +
        "local rate = tonumber(ARGV[2]) " +
        "local now = tonumber(ARGV[3]) " +
        "local requested = tonumber(ARGV[4]) " +
        "local ttl = tonumber(ARGV[5]) " +
        "local bucket = redis.call('HMGET', key, 'tokens', 'lastRefill') " +
        "local tokens = tonumber(bucket[1]) " +
        "local lastRefill = tonumber(bucket[2]) " +
        "if tokens == nil then " +
        "  tokens = capacity " +
        "  lastRefill = now " +
        "end " +
        "local elapsed = math.max(now - lastRefill, 0) " +
        "local newTokens = math.min(capacity, tokens + elapsed * rate / 1000) " +
        "if newTokens < requested then " +
        "  return 0 " +
        "end " +
        "newTokens = newTokens - requested " +
        "redis.call('HMSET', key, 'tokens', newTokens, 'lastRefill', now) " +
        "redis.call('EXPIRE', key, ttl) " +
        "return 1";

    /**
     * 令牌桶取令牌
     * @param key 桶标识
     * @param capacity 桶容量
     * @param rate 每秒生成令牌数
     * @param requested 请求令牌数
     * @return true-获取成功 false-被限流
     */
    public boolean tryAcquireToken(String key, long capacity, long rate, long requested) {
        long now = Instant.now().toEpochMilli();
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(TOKEN_BUCKET_LUA, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key),
                String.valueOf(capacity), String.valueOf(rate), String.valueOf(now),
                String.valueOf(requested), "60");
        return result != null && result == 1L;
    }
}