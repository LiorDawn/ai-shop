package org.example.aishop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Redis Lua 脚本预加载配置
 * 脚本在启动时加载到 Redis 服务端缓存，避免每次请求上传脚本消耗带宽
 */
@Configuration
public class RedisScriptConfig {

    /** 扣减库存（含优惠券） */
    @Bean("deductStockScript")
    public RedisScript<Long> deductStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/deduct_stock.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /** 库存回滚 */
    @Bean("rollbackStockScript")
    public RedisScript<Long> rollbackStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/rollback_stock.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /** 领取优惠券 */
    @Bean("claimCouponScript")
    public RedisScript<Long> claimCouponScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/claim_coupon.lua")));
        script.setResultType(Long.class);
        return script;
    }
}