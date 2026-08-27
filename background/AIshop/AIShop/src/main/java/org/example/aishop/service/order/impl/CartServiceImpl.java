package org.example.aishop.service.order.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.dto.CartItemVO;
import org.example.aishop.dto.CartSettleVO;
import org.example.aishop.entity.order.Cart;
import org.example.aishop.entity.product.Product;
import org.example.aishop.entity.product.ProductSku;
import org.example.aishop.entity.merchant.Shop;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.order.CartMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mapper.product.ProductSkuMapper;
import org.example.aishop.mapper.merchant.ShopMapper;
import org.example.aishop.service.order.CartService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== 内部数据类：Redis Hash 中存储的购物车条目 ==========

    private static class CartItemCache {
        public Long productId;
        public Long skuId;
        public Integer num;
        public Integer checked;
        public Long createTime;

        public CartItemCache() {}

        public CartItemCache(Long productId, Long skuId, Integer num, Integer checked) {
            this.productId = productId;
            this.skuId = skuId;
            this.num = num;
            this.checked = checked;
            this.createTime = System.currentTimeMillis();
        }
    }

    // ========== 一、添加购物车（Redis + MySQL 双写） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long productId, Long skuId, Integer num) {
        Long userId = UserHolder.getUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");
        if (productId == null) throw new BusinessException(400, "商品ID不能为空");
        if (num == null || num <= 0) num = 1;

        // 校验商品是否存在且上架
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException(400, "商品已下架或不存在");
        }

        String key = RedisConstant.cartUserKey(userId);
        String field = String.valueOf(productId);

        // 查询 Redis 是否已有该商品
        String existingJson = (String) stringRedisTemplate.opsForHash().get(key, field);

        if (existingJson != null) {
            // 分支1：Redis 已存在 → 叠加数量，更新 Redis + MySQL
            try {
                CartItemCache item = objectMapper.readValue(existingJson, CartItemCache.class);
                item.num = item.num + num;
                // 如果传入了 skuId 则覆盖
                if (skuId != null) item.skuId = skuId;
                stringRedisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(item));
            } catch (Exception e) {
                throw new BusinessException(500, "购物车操作失败");
            }

            // 同步 MySQL：更新 num, checked
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getUserId, userId).eq(Cart::getProductId, productId);
            Cart existingCart = cartMapper.selectOne(wrapper, false);
            if (existingCart != null) {
                existingCart.setNum(existingCart.getNum() + num);
                existingCart.setChecked(1);
                if (skuId != null) existingCart.setSkuId(skuId);
                cartMapper.updateById(existingCart);
            }
        } else {
            // 分支2：Redis 无该商品 → 新建，写入 Redis + MySQL
            // 未指定 SKU 时取默认
            if (skuId == null) skuId = getDefaultSkuId(productId);
            CartItemCache item = new CartItemCache(productId, skuId, num, 1);
            try {
                stringRedisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(item));
            } catch (Exception e) {
                throw new BusinessException(500, "购物车操作失败");
            }

            // 同步 MySQL：insert
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setSkuId(skuId);
            cart.setNum(num);
            cart.setChecked(1);
            cart.setCreateTime(new Date());
            cartMapper.insert(cart);
        }

        // 刷新 Redis 过期时间
        stringRedisTemplate.expire(key, RedisConstant.CART_TTL_SECONDS, TimeUnit.SECONDS);
    }

    // ========== 二、查询购物车列表（Redis 优先 + MySQL 兜底回填） ==========

    @Override
    public List<CartItemVO> listCart() {
        Long userId = UserHolder.getUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        String key = RedisConstant.cartUserKey(userId);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);

        List<CartItemCache> cacheItems;

        if (entries != null && !entries.isEmpty()) {
            // 分支1：Redis 有数据 → 直接解析
            cacheItems = new ArrayList<>();
            for (Object value : entries.values()) {
                try {
                    CartItemCache item = objectMapper.readValue((String) value, CartItemCache.class);
                    cacheItems.add(item);
                } catch (Exception ignored) {}
            }
        } else {
            // 分支2：Redis 无数据 → 查 MySQL 并回填 Redis
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getUserId, userId);
            List<Cart> dbList = cartMapper.selectList(wrapper);

            cacheItems = new ArrayList<>();
            if (dbList != null) {
                for (Cart cart : dbList) {
                    CartItemCache item = new CartItemCache(
                            cart.getProductId(),
                            cart.getSkuId(),
                            cart.getNum() != null ? cart.getNum() : 1,
                            cart.getChecked() != null ? cart.getChecked() : 1
                    );
                    item.createTime = cart.getCreateTime() != null ? cart.getCreateTime().getTime() : System.currentTimeMillis();
                    cacheItems.add(item);

                    // 回填 Redis
                    try {
                        stringRedisTemplate.opsForHash().put(key,
                                String.valueOf(cart.getProductId()),
                                objectMapper.writeValueAsString(item));
                    } catch (Exception ignored) {}
                }
                // 刷新过期时间
                if (!cacheItems.isEmpty()) {
                    stringRedisTemplate.expire(key, RedisConstant.CART_TTL_SECONDS, TimeUnit.SECONDS);
                }
            }
        }

        if (cacheItems.isEmpty()) return Collections.emptyList();

        return assembleCartVOs(cacheItems, userId);
    }

    /**
     * 根据 CacheItem 列表组装 CartItemVO（批量查商品、店铺）
     */
    private List<CartItemVO> assembleCartVOs(List<CartItemCache> cacheItems, Long userId) {
        // 批量查商品
        Set<Long> productIds = cacheItems.stream().map(i -> i.productId).collect(Collectors.toSet());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 批量查 SKU
        Set<Long> skuIds = cacheItems.stream().map(i -> i.skuId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ProductSku> skuMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            List<ProductSku> skuList = productSkuMapper.selectBatchIds(skuIds);
            skuMap = skuList.stream().collect(Collectors.toMap(ProductSku::getId, s -> s));
        }

        // 批量查店铺
        Set<Long> shopIds = products.stream().map(Product::getShopId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Shop> shopMap = Collections.emptyMap();
        if (!shopIds.isEmpty()) {
            List<Shop> shops = shopMapper.selectBatchIds(shopIds);
            shopMap = shops.stream().collect(Collectors.toMap(Shop::getId, s -> s));
        }

        // 组装 VO
        List<CartItemVO> vos = new ArrayList<>();
        for (CartItemCache item : cacheItems) {
            Product product = productMap.get(item.productId);
            if (product == null) continue;

            // SKU 价格优先
            ProductSku sku = item.skuId != null ? skuMap.get(item.skuId) : null;
            BigDecimal price = (sku != null && sku.getPrice() != null) ? sku.getPrice() : product.getPrice();

            CartItemVO vo = new CartItemVO();
            vo.setId(item.productId); // 用 productId 作为 cartId
            vo.setUserId(userId);
            vo.setProductId(item.productId);
            vo.setSkuId(item.skuId);
            vo.setSpec(sku != null ? sku.getSpec() : null);
            vo.setProductName(product.getName());
            vo.setProductImage(product.getImage());
            vo.setPrice(price);
            vo.setNum(item.num);
            vo.setChecked(item.checked != null ? item.checked : 1);
            vo.setProductStatus(product.getStatus());
            vo.setShopId(product.getShopId());
            vo.setCreateTime(item.createTime != null ? new Date(item.createTime) : new Date());

            if (product.getShopId() != null) {
                Shop shop = shopMap.get(product.getShopId());
                vo.setShopName(shop != null ? shop.getShopName() : "未知店铺");
            } else {
                vo.setShopName("未知店铺");
            }

            vos.add(vo);
        }
        return vos;
    }

    // ========== 三、修改数量（Redis + MySQL 同步） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNum(Long cartId, Integer num) {
        Long userId = UserHolder.getUserId();
        if (cartId == null || num == null || num < 1) {
            throw new BusinessException(400, "参数错误");
        }

        String key = RedisConstant.cartUserKey(userId);
        String field = String.valueOf(cartId);

        // 更新 Redis
        String existingJson = (String) stringRedisTemplate.opsForHash().get(key, field);
        if (existingJson == null) {
            throw new BusinessException(403, "无权操作");
        }
        try {
            CartItemCache item = objectMapper.readValue(existingJson, CartItemCache.class);
            item.num = num;
            stringRedisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(item));
        } catch (Exception e) {
            throw new BusinessException(500, "操作失败");
        }

        // 同步 MySQL
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId).eq(Cart::getProductId, cartId);
        Cart existingCart = cartMapper.selectOne(wrapper, false);
        if (existingCart != null) {
            existingCart.setNum(num);
            cartMapper.updateById(existingCart);
        }

        // 刷新过期时间
        stringRedisTemplate.expire(key, RedisConstant.CART_TTL_SECONDS, TimeUnit.SECONDS);
    }

    // ========== 四、勾选/取消勾选（Redis + MySQL 同步） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleCheck(Long cartId, Integer checked) {
        Long userId = UserHolder.getUserId();
        String key = RedisConstant.cartUserKey(userId);
        String field = String.valueOf(cartId);

        // 更新 Redis
        String existingJson = (String) stringRedisTemplate.opsForHash().get(key, field);
        if (existingJson == null) {
            throw new BusinessException(403, "无权操作");
        }
        try {
            CartItemCache item = objectMapper.readValue(existingJson, CartItemCache.class);
            item.checked = checked != null ? checked : 1;
            stringRedisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(item));
        } catch (Exception e) {
            throw new BusinessException(500, "操作失败");
        }

        // 同步 MySQL
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId).eq(Cart::getProductId, cartId);
        Cart existingCart = cartMapper.selectOne(wrapper, false);
        if (existingCart != null) {
            existingCart.setChecked(checked != null ? checked : 1);
            cartMapper.updateById(existingCart);
        }
    }

    // ========== 全选/全不选（Redis + MySQL 同步） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAll(Integer checked) {
        Long userId = UserHolder.getUserId();
        String key = RedisConstant.cartUserKey(userId);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) return;

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                CartItemCache item = objectMapper.readValue((String) entry.getValue(), CartItemCache.class);
                item.checked = checked;
                stringRedisTemplate.opsForHash().put(key, entry.getKey(), objectMapper.writeValueAsString(item));
            } catch (Exception ignored) {}
        }

        // 同步 MySQL：批量更新所有项的 checked
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        List<Cart> carts = cartMapper.selectList(wrapper);
        for (Cart cart : carts) {
            cart.setChecked(checked);
            cartMapper.updateById(cart);
        }
    }

    // ========== 五、删除单个（Redis + MySQL 同步删除） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long cartId) {
        Long userId = UserHolder.getUserId();
        String key = RedisConstant.cartUserKey(userId);

        // 删除 Redis
        stringRedisTemplate.opsForHash().delete(key, String.valueOf(cartId));

        // 同步删除 MySQL
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId).eq(Cart::getProductId, cartId);
        cartMapper.delete(wrapper);
    }

    // ========== 辅助方法：查询默认 SKU ==========
    private Long getDefaultSkuId(Long productId) {
        LambdaQueryWrapper<ProductSku> skuQuery = new LambdaQueryWrapper<>();
        skuQuery.eq(ProductSku::getProductId, productId).last("LIMIT 1");
        ProductSku sku = productSkuMapper.selectOne(skuQuery);
        return sku != null ? sku.getId() : productId;
    }

    // ========== 批量删除（Redis + MySQL 同步删除） ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> cartIds) {
        Long userId = UserHolder.getUserId();
        if (cartIds == null || cartIds.isEmpty()) return;

        String key = RedisConstant.cartUserKey(userId);

        // 删除 Redis
        Object[] fields = cartIds.stream().map(String::valueOf).toArray();
        stringRedisTemplate.opsForHash().delete(key, fields);

        // 同步删除 MySQL
        for (Long productId : cartIds) {
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getUserId, userId).eq(Cart::getProductId, productId);
            cartMapper.delete(wrapper);
        }
    }

    // ========== 六、结算前置校验（按店铺分组） ==========

    @Override
    public CartSettleVO settleCheck() {
        List<CartItemVO> vos = listCart();

        // 筛出有效且选中的商品
        List<CartItemVO> checkedItems = vos.stream()
                .filter(item -> item.getChecked() != null && item.getChecked() == 1)
                .filter(item -> item.getProductStatus() != null && item.getProductStatus() == 1)
                .collect(Collectors.toList());

        if (checkedItems.isEmpty()) {
            throw new BusinessException(400, "请至少勾选一件有效商品");
        }

        // 按店铺分组
        Map<Long, List<CartItemVO>> shopGroupMap = checkedItems.stream()
                .collect(Collectors.groupingBy(item ->
                        item.getShopId() != null ? item.getShopId() : 0L));

        List<CartSettleVO.ShopCartGroup> groups = new ArrayList<>();
        for (Map.Entry<Long, List<CartItemVO>> entry : shopGroupMap.entrySet()) {
            CartSettleVO.ShopCartGroup group = new CartSettleVO.ShopCartGroup();
            group.setShopId(entry.getKey());
            group.setShopName(entry.getValue().get(0).getShopName());
            group.setItems(entry.getValue());
            groups.add(group);
        }

        int totalNum = checkedItems.stream().mapToInt(CartItemVO::getNum).sum();
        BigDecimal totalPrice = checkedItems.stream()
                .map(CartItemVO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartSettleVO settleVO = new CartSettleVO();
        settleVO.setShopGroups(groups);
        settleVO.setTotalNum(totalNum);
        settleVO.setTotalPrice(totalPrice);
        settleVO.setCrossShop(groups.size() > 1);
        return settleVO;
    }

    // ========== 七、下单后清空购物车（Redis + MySQL 同步清理） ==========

    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        // 删除 Redis
        stringRedisTemplate.delete(RedisConstant.cartUserKey(userId));

        // 删除 MySQL 该用户全部购物车数据
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        cartMapper.delete(wrapper);
    }

    /**
     * 下单后清理指定商品（只删除已结算的商品，保留未选中的）
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearCartByProductIds(Long userId, Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        String key = RedisConstant.cartUserKey(userId);

        // 删除 Redis 中已结算的商品
        Object[] fields = productIds.stream().map(String::valueOf).toArray();
        stringRedisTemplate.opsForHash().delete(key, fields);

        // 同步删除 MySQL
        for (Long productId : productIds) {
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getUserId, userId).eq(Cart::getProductId, productId);
            cartMapper.delete(wrapper);
        }
    }
}