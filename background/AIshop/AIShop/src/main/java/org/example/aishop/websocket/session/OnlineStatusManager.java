package org.example.aishop.websocket.session;

import org.example.aishop.common.constant.RedisConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 在线状态管理器
 * <p>
 * 统一管理买家、商家、管理员的在线状态标记，使用 Redis Set 存储。
 * 所有 Redis 异常在此层统一捕获处理，不向上抛出。
 */
@Component
public class OnlineStatusManager {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final long EXPIRE_DAYS = 7;

    /**
     * Redis Set 值前缀，区分 user.id 和 merchant.id 两种 ID 类型。
     * 避免 user.id=200 和 merchant.id=200 在同一个 Set 中冲突。
     */
    private static final String UID_PREFIX = "uid:";
    private static final String MID_PREFIX = "mid:";

    // ==================== 标记在线 ====================

    public void markMerchantOnline(Long uid) {
        safeAdd(RedisConstant.ONLINE_MERCHANT_PREFIX, UID_PREFIX + uid);
    }

    public void markMerchantIdOnline(Long merchantId) {
        safeAdd(RedisConstant.ONLINE_MERCHANT_PREFIX, MID_PREFIX + merchantId);
    }

    public void markBuyerOnline(Long uid) {
        safeAdd(RedisConstant.ONLINE_USER_PREFIX, uid.toString());
    }

    public void markAdminOnline(Long uid) {
        safeAdd(RedisConstant.ONLINE_PLATFORM_ADMIN_KEY, uid.toString());
    }

    // ==================== 标记离线 ====================

    public void markMerchantOffline(Long uid) {
        safeRemove(RedisConstant.ONLINE_MERCHANT_PREFIX, UID_PREFIX + uid);
    }

    public void markMerchantIdOffline(Long merchantId) {
        safeRemove(RedisConstant.ONLINE_MERCHANT_PREFIX, MID_PREFIX + merchantId);
    }

    public void markBuyerOffline(Long uid) {
        safeRemove(RedisConstant.ONLINE_USER_PREFIX, uid.toString());
    }

    public void markAdminOffline(Long uid) {
        safeRemove(RedisConstant.ONLINE_PLATFORM_ADMIN_KEY, uid.toString());
    }

    // ==================== 查询在线状态 ====================

    public boolean isMerchantOnline(Long merchantId) {
        return safeIsMember(RedisConstant.ONLINE_MERCHANT_PREFIX, MID_PREFIX + merchantId);
    }

    /** 获取所有在线管理员 */
    public Set<String> getOnlineAdmins() {
        try {
            return stringRedisTemplate.opsForSet().members(RedisConstant.ONLINE_PLATFORM_ADMIN_KEY);
        } catch (Exception e) {
            System.err.println("[OnlineStatus] 查询在线管理员失败: " + e.getMessage());
            return null;
        }
    }

    // ==================== 内部安全方法 ====================

    private void safeAdd(String key, String value) {
        try {
            stringRedisTemplate.opsForSet().add(key, value);
            stringRedisTemplate.expire(key, EXPIRE_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            System.err.println("[OnlineStatus] Redis 标记在线失败: key=" + key + ", " + e.getMessage());
        }
    }

    private void safeRemove(String key, String value) {
        try {
            stringRedisTemplate.opsForSet().remove(key, value);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("destroyed")) {
                System.out.println("[OnlineStatus] 应用关闭中，跳过Redis操作");
            } else {
                System.err.println("[OnlineStatus] Redis 删除在线状态失败: key=" + key + ", " + e.getMessage());
            }
        }
    }

    private boolean safeIsMember(String key, String value) {
        try {
            Boolean member = stringRedisTemplate.opsForSet().isMember(key, value);
            return Boolean.TRUE.equals(member);
        } catch (Exception e) {
            System.err.println("[OnlineStatus] Redis 查询在线状态失败: " + e.getMessage());
            return false;
        }
    }
}