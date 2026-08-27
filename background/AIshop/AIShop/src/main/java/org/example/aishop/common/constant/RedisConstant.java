package org.example.aishop.common.constant;

/**
 * Redis Key 和过期时间常量
 */
public interface RedisConstant {

    // ==================== Key 前缀 ====================
    /** 商品详情缓存 Key 前缀 */
    String PRODUCT_DETAIL_PREFIX = "AISHOP:PRODUCT:DETAIL:";
    /** 分类商品列表缓存 Key 前缀 */
    String PRODUCT_LIST_CATEGORY_PREFIX = "AISHOP:PRODUCT:LIST:CATEGORY:";
    /** 店铺商品列表缓存 Key 前缀 */
    String PRODUCT_LIST_SHOP_PREFIX = "AISHOP:PRODUCT:LIST:SHOP:";
    /** 商品浏览量 Key 前缀 */
    String PRODUCT_VIEW_PREFIX = "AISHOP:PRODUCT:VIEW:";
    /** 热门商品排行榜 ZSet Key */
    String PRODUCT_HOT_RANK_KEY = "AISHOP:PRODUCT:HOT_RANK";
    /** 库存分布式锁 Key 前缀 */
    String STOCK_LOCK_PREFIX = "AISHOP:LOCK:STOCK:";
    /** 分类树缓存 Key */
    String CATEGORY_TREE_KEY = "AISHOP:CATEGORY:TREE";
    /** 购物车 Key 前缀 */
    String CART_USER_PREFIX = "AISHOP:CART:USER:";
    /** 用户收藏集合 Key 前缀 */
    String COLLECT_USER_PREFIX = "AISHOP:COLLECT:USER:";
    /** 用户信息缓存 Key 前缀 */
    String USER_INFO_PREFIX = "AISHOP:USER:INFO:";
    /** 用户 Token Key 前缀 */
    String USER_TOKEN_PREFIX = "AISHOP:USER:TOKEN:";
    /** 上传限流 Key 前缀 */
    String UPLOAD_RATE_LIMIT_PREFIX = "AISHOP:UPLOAD:LIMIT:";
    /** 管理端统计大盘缓存 Key */
    String STATS_ADMIN_OVERVIEW_KEY = "AISHOP:STATS:ADMIN:OVERVIEW";
    /** 商家端统计仪表盘缓存 Key 前缀 */
    String STATS_MERCHANT_OVERVIEW_PREFIX = "AISHOP:STATS:MERCHANT:OVERVIEW:";
    /** 商家端热销排行缓存 Key 前缀 */
    String STATS_SALES_RANKING_PREFIX = "AISHOP:STATS:MERCHANT:RANKING:";
    /** 商家端订单趋势缓存 Key 前缀 */
    String STATS_ORDER_TREND_PREFIX = "AISHOP:STATS:MERCHANT:TREND:";
    /** SKU 库存缓存 Key 前缀 */
    String STOCK_SKU_PREFIX = "AISHOP:STOCK:SKU:";
    /** 优惠券库存缓存 Key 前缀 */
    String COUPON_STOCK_PREFIX = "AISHOP:COUPON:STOCK:";
    /** 用户已领优惠券 Set Key 前缀 */
    String COUPON_USER_SET_PREFIX = "AISHOP:COUPON:USER:";
    /** 评论分页缓存 Key 前缀 */
    String COMMENT_PAGE_PREFIX = "AISHOP:COMMENT:PAGE:";
    /** 评论评分统计 Key 前缀 */
    String COMMENT_RATING_PREFIX = "AISHOP:COMMENT:RATING:";
    /** 下单接口限流 Key 前缀 */
    String ORDER_RATE_LIMIT_PREFIX = "AISHOP:ORDER:LIMIT:";
    /** 热点订单详情缓存 Key 前缀 */
    String ORDER_DETAIL_PREFIX = "AISHOP:ORDER:DETAIL:";
    /** 销量同步锁 Key（防止重复同步） */
    String SALES_SYNC_KEY = "AISHOP:SALES:SYNC";
    /** AI 会话上下文缓存 Key 前缀 */
    String AI_CONTEXT_PREFIX = "AISHOP:AI:CONTEXT:";
    /** AI 会话列表缓存 Key 前缀 */
    String AI_SESSION_LIST_PREFIX = "AISHOP:AI:SESSION:LIST:";
    /** 在线用户集合 Key */
    String ONLINE_USER_PREFIX = "AISHOP:ONLINE:USER:";
    /** 在线商家集合 Key */
    String ONLINE_MERCHANT_PREFIX = "AISHOP:ONLINE:MERCHANT:";
    /** 在线平台管理员集合 Key */
    String ONLINE_PLATFORM_ADMIN_KEY = "AISHOP:ONLINE:PLATFORM_ADMIN";
    /** 离线消息队列 Key 前缀 */
    String OFFLINE_MSG_PREFIX = "AISHOP:OFFLINE:MSG:";
    /** 离线平台客服消息队列 Key 前缀 */
    String OFFLINE_PLATFORM_MSG_PREFIX = "AISHOP:OFFLINE:PLATFORM:";

    // ==================== 验证码相关 ====================
    /** 手机验证码缓存 Key 前缀 */
    String SMS_CODE_PREFIX = "AISHOP:SMS:CODE:";
    /** 邮箱验证码缓存 Key 前缀 */
    String EMAIL_CODE_PREFIX = "AISHOP:EMAIL:CODE:";
    /** 短信发送限流 Key 前缀 */
    String SMS_LIMIT_PREFIX = "AISHOP:SMS:LIMIT:";
    /** 邮件发送限流 Key 前缀 */
    String EMAIL_LIMIT_PREFIX = "AISHOP:EMAIL:LIMIT:";
    /** 验证码有效期：5分钟 */
    long VERIFY_CODE_TTL_SECONDS = 5 * 60;
    /** 限流窗口：5分钟 */
    long LIMIT_WINDOW_SECONDS = 5 * 60;
    /** 限流最大次数 */
    long LIMIT_MAX_COUNT = 3;

    // ==================== 过期时间 ====================
    /** 商品详情缓存有效期：30分钟 */
    long PRODUCT_DETAIL_TTL_SECONDS = 30 * 60;
    /** 分类商品列表缓存有效期：10分钟 */
    long PRODUCT_LIST_TTL_SECONDS = 10 * 60;
    /** 分类树缓存有效期：1小时 */
    long CATEGORY_TREE_TTL_SECONDS = 60 * 60;
    /** 用户信息缓存有效期：30分钟 */
    long USER_INFO_TTL_SECONDS = 30 * 60;
    /** 用户 Token 缓存有效期：7天 */
    long USER_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60;
    /** 购物车缓存有效期：7天 */
    long CART_TTL_SECONDS = 7 * 24 * 60 * 60;
    /** 收藏缓存有效期：30天 */
    long COLLECT_TTL_SECONDS = 30 * 24 * 60 * 60;
    /** 浏览量同步间隔：1小时 */
    long VIEW_SYNC_TTL_SECONDS = 60 * 60;
    /** 分布式锁超时时间：10秒 */
    long STOCK_LOCK_TIMEOUT_SECONDS = 10;
    /** 上传限流窗口：1分钟 */
    long UPLOAD_RATE_LIMIT_SECONDS = 60;
    /** 上传限流最大次数 */
    long UPLOAD_RATE_LIMIT_MAX = 10;
    /** 统计数据缓存有效期：30分钟 */
    long STATS_CACHE_TTL_SECONDS = 30 * 60;
    /** 评论分页缓存有效期：10分钟 */
    long COMMENT_PAGE_TTL_SECONDS = 10 * 60;
    /** 评论评分缓存有效期：30分钟 */
    long COMMENT_RATING_TTL_SECONDS = 30 * 60;
    /** 订单详情缓存有效期：30分钟 */
    long ORDER_DETAIL_TTL_SECONDS = 30 * 60;
    /** 下单限流窗口：1秒 */
    long ORDER_RATE_LIMIT_SECONDS = 1;
    /** 下单限流最大次数（每秒） */
    long ORDER_RATE_LIMIT_MAX = 50;
    /** AI 会话上下文缓存有效期：30分钟 */
    long AI_CONTEXT_TTL_SECONDS = 30 * 60;

    // ==================== 缓存防护相关 ====================
    /** 空值缓存有效期：1分钟（防穿透） */
    long NULL_CACHE_TTL_SECONDS = 1 * 60;
    /** 缓存重建互斥锁 Key 前缀 */
    String CACHE_LOCK_PREFIX = "AISHOP:LOCK:CACHE:";
    /** 缓存重建互斥锁超时时间：10秒 */
    long CACHE_LOCK_TIMEOUT_SECONDS = 10;
    /** 缓存重建互斥锁等待重试间隔：50毫秒 */
    long CACHE_LOCK_RETRY_MS = 50;
    /** TTL 随机偏移上限：5分钟（防雪崩） */
    long TTL_RANDOM_MAX_SECONDS = 5 * 60;
    /** 空值缓存标记 */
    String NULL_CACHE_MARKER = "__NULL__";

    // ==================== Key 构建方法 ====================

    static String productDetailKey(Long productId) {
        return PRODUCT_DETAIL_PREFIX + productId;
    }

    static String productListCategoryKey(Long categoryId) {
        return PRODUCT_LIST_CATEGORY_PREFIX + categoryId;
    }

    static String productListShopKey(Long shopId) {
        return PRODUCT_LIST_SHOP_PREFIX + shopId;
    }

    static String productViewKey(Long productId) {
        return PRODUCT_VIEW_PREFIX + productId;
    }

    static String stockLockKey(Long skuId) {
        return STOCK_LOCK_PREFIX + skuId;
    }

    static String cartUserKey(Long userId) {
        return CART_USER_PREFIX + userId;
    }

    static String collectUserKey(Long userId) {
        return COLLECT_USER_PREFIX + userId;
    }

    static String userInfoKey(Long userId) {
        return USER_INFO_PREFIX + userId;
    }

    static String userTokenKey(Long userId) {
        return USER_TOKEN_PREFIX + userId;
    }

    static String uploadRateLimitKey(String ip) {
        return UPLOAD_RATE_LIMIT_PREFIX + ip;
    }

    static String statsMerchantOverviewKey(Long shopId) {
        return STATS_MERCHANT_OVERVIEW_PREFIX + shopId;
    }

    static String statsSalesRankingKey(Long shopId) {
        return STATS_SALES_RANKING_PREFIX + shopId;
    }

    static String statsOrderTrendKey(Long shopId) {
        return STATS_ORDER_TREND_PREFIX + shopId;
    }

    static String stockSkuKey(Long skuId) {
        return STOCK_SKU_PREFIX + skuId;
    }

    static String couponStockKey(Long couponId) {
        return COUPON_STOCK_PREFIX + couponId;
    }

    static String couponUserSetKey(Long couponId) {
        return COUPON_USER_SET_PREFIX + couponId;
    }

    static String commentPageKey(Long productId, Integer page) {
        return COMMENT_PAGE_PREFIX + productId + ":" + page;
    }

    static String commentRatingKey(Long productId) {
        return COMMENT_RATING_PREFIX + productId;
    }

    static String orderRateLimitKey(Long userId) {
        return ORDER_RATE_LIMIT_PREFIX + userId;
    }

    static String orderDetailKey(Long orderId) {
        return ORDER_DETAIL_PREFIX + orderId;
    }

    static String aiContextKey(Long sessionId) {
        return AI_CONTEXT_PREFIX + sessionId;
    }

    static String aiSessionListKey(Long userId) {
        return AI_SESSION_LIST_PREFIX + userId;
    }

    static String onlineUserKey(Long userId) {
        return ONLINE_USER_PREFIX + userId;
    }

    static String onlineMerchantKey(Long merchantId) {
        return ONLINE_MERCHANT_PREFIX + merchantId;
    }

    static String offlineMsgKey(String targetKey) {
        return OFFLINE_MSG_PREFIX + targetKey;
    }

    static String offlinePlatformMsgKey(String targetKey) {
        return OFFLINE_PLATFORM_MSG_PREFIX + targetKey;
    }

    static String cacheLockKey(String cacheKey) {
        return CACHE_LOCK_PREFIX + cacheKey;
    }

    }