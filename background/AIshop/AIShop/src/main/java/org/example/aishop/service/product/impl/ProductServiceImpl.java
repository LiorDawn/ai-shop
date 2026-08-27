package org.example.aishop.service.product.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.service.ai.AIContentService;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.dto.ProductDTO;
import org.example.aishop.dto.ProductImageDTO;
import org.example.aishop.dto.ProductSkuDTO;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.entity.merchant.Shop;
import org.example.aishop.entity.product.Category;
import org.example.aishop.entity.product.Product;
import org.example.aishop.entity.product.ProductImage;
import org.example.aishop.entity.product.ProductSku;
import org.example.aishop.mapper.merchant.MerchantMapper;
import org.example.aishop.mapper.merchant.ShopMapper;
import org.example.aishop.mapper.product.CategoryMapper;
import org.example.aishop.mapper.product.ProductImageMapper;
import org.example.aishop.mapper.product.ProductMapper;
import org.example.aishop.mapper.product.ProductSkuMapper;
import org.example.aishop.service.product.ProductService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AIContentService aiContentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取当前登录商家对应的店铺ID
     * 直接从数据库查询，不依赖缓存
     */
    public Long getCurrentMerchantShopId() {
        Long userId = UserHolder.getUserId();
        if (userId == null) return null;
        try {
            // 当前用户 → Merchant 记录 → Shop 记录
            Merchant merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, userId));
            if (merchant == null) return null;
            // 审核通过时已同步创建店铺，直接查询
            Shop shop = shopMapper.selectOne(
                    new LambdaQueryWrapper<Shop>().eq(Shop::getMerchantId, merchant.getId()));
            if (shop == null) {
                // 极低概率：店铺未创建则主动创建
                shop = new Shop();
                shop.setMerchantId(merchant.getId());
                shop.setShopName(merchant.getMerchantName());
                shop.setStatus(1);
                shopMapper.insert(shop);
            }
            return shop.getId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(ProductDTO productDTO) {
        if (!StringUtils.hasText(productDTO.getName())) {
            throw new BusinessException(400, "商品名称不能为空");
        }
        if (productDTO.getCategoryId() == null) {
            throw new BusinessException(400, "商品分类不能为空");
        }
        if (productDTO.getPrice() == null) {
            throw new BusinessException(400, "商品价格不能为空");
        }

        // 商家自动注入所属店铺ID
        if (productDTO.getShopId() == null) {
            Long shopId = getCurrentMerchantShopId();
            if (shopId != null) {
                productDTO.setShopId(shopId);
            }
        }
        if (productDTO.getShopId() == null) {
            throw new BusinessException(400, "所属店铺不能为空");
        }

        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);

        // AI 智能审核商品名称（检查开关）
        if (productDTO.getName() != null) {
            String reviewEnabled = stringRedisTemplate.opsForValue().get("AISHOP:CONFIG:AI_REVIEW_ENABLED");
            if (!"false".equals(reviewEnabled)) {
                String reviewResult = aiContentService.reviewContent(productDTO.getName(), "商品名称");
                if (reviewResult != null) {
                    throw new BusinessException(400, "商品名称包含违规内容，请修改后重试");
                }
            }
        }

        super.save(product);

        // 清除缓存，使用户端立即可见新商品
        clearProductListCache();

        // 保存主图到商品表的 image 字段（已在 productDTO.image 中）
        // 保存轮播图
        if (productDTO.getImageList() != null && !productDTO.getImageList().isEmpty()) {
            for (ProductImageDTO imgDTO : productDTO.getImageList()) {
                ProductImage pi = new ProductImage();
                pi.setProductId(product.getId());
                pi.setImageUrl(imgDTO.getImageUrl());
                pi.setSort(0);
                productImageMapper.insert(pi);
            }
        }

        // 保存SKU
        if (productDTO.getSkuList() != null && !productDTO.getSkuList().isEmpty()) {
            for (ProductSkuDTO skuDTO : productDTO.getSkuList()) {
                ProductSku ps = new ProductSku();
                ps.setProductId(product.getId());
                ps.setSpec(skuDTO.getSpec());
                ps.setPrice(skuDTO.getPrice());
                ps.setStock(skuDTO.getStock());
                productSkuMapper.insert(ps);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductDTO productDTO) {
        if (productDTO.getId() == null) {
            throw new BusinessException(400, "商品ID不能为空");
        }
        Product exist = super.getById(productDTO.getId());
        if (exist == null) {
            throw new BusinessException(404, "商品不存在");
        }
        // 校验权限：只能操作自己店铺的商品
        checkProductOwnership(productDTO.getId());

        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        super.updateById(product);

        // 清除缓存，使用户端立即可见变更
        clearProductListCache();

        // 删除旧轮播图重新插入
        LambdaQueryWrapper<ProductImage> imgWrapper = new LambdaQueryWrapper<>();
        imgWrapper.eq(ProductImage::getProductId, product.getId());
        productImageMapper.delete(imgWrapper);
        if (productDTO.getImageList() != null && !productDTO.getImageList().isEmpty()) {
            for (ProductImageDTO imgDTO : productDTO.getImageList()) {
                ProductImage pi = new ProductImage();
                pi.setProductId(product.getId());
                pi.setImageUrl(imgDTO.getImageUrl());
                pi.setSort(0);
                productImageMapper.insert(pi);
            }
        }

        // 删除旧SKU重新插入
        LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.eq(ProductSku::getProductId, product.getId());
        productSkuMapper.delete(skuWrapper);
        if (productDTO.getSkuList() != null && !productDTO.getSkuList().isEmpty()) {
            for (ProductSkuDTO skuDTO : productDTO.getSkuList()) {
                ProductSku ps = new ProductSku();
                ps.setProductId(product.getId());
                ps.setSpec(skuDTO.getSpec());
                ps.setPrice(skuDTO.getPrice());
                ps.setStock(skuDTO.getStock());
                productSkuMapper.insert(ps);
            }
        }

        // 清除 Redis 缓存
        deleteProductCache(product.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "商品不存在");
        }
        // 校验权限
        checkProductOwnership(id);
        // 删除关联轮播图和SKU
        LambdaQueryWrapper<ProductImage> imgWrapper = new LambdaQueryWrapper<>();
        imgWrapper.eq(ProductImage::getProductId, id);
        productImageMapper.delete(imgWrapper);

        LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.eq(ProductSku::getProductId, id);
        productSkuMapper.delete(skuWrapper);

        super.removeById(id);

        // 清除 Redis 缓存
        deleteProductCache(id);
        clearProductListCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的商品");
        }
        for (Long id : ids) {
            checkProductOwnership(id);
            LambdaQueryWrapper<ProductImage> imgWrapper = new LambdaQueryWrapper<>();
            imgWrapper.eq(ProductImage::getProductId, id);
            productImageMapper.delete(imgWrapper);

            LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
            skuWrapper.eq(ProductSku::getProductId, id);
            productSkuMapper.delete(skuWrapper);
        }
        super.removeByIds(ids);

        // 批量清除 Redis 缓存
        for (Long id : ids) {
            deleteProductCache(id);
        }
    }

    @Override
    public void upProduct(Long id) {
        Product exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "商品不存在");
        }
        checkProductOwnership(id);
        Product product = new Product();
        product.setId(id);
        product.setStatus(1);
        super.updateById(product);

        // 清除 Redis 缓存
        deleteProductCache(id);
    }

    @Override
    public void downProduct(Long id) {
        Product exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "商品不存在");
        }
        checkProductOwnership(id);
        Product product = new Product();
        product.setId(id);
        product.setStatus(0);
        super.updateById(product);

        // 清除 Redis 缓存
        deleteProductCache(id);
    }

    @Override
    public ProductDTO getProductById(Long id) {
        String cacheKey = RedisConstant.productDetailKey(id);

        // 1. 先查 Redis 缓存
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            // 1a. 空值缓存命中 → 防穿透，直接返回
            if (RedisConstant.NULL_CACHE_MARKER.equals(cachedJson)) {
                throw new BusinessException(404, "商品不存在");
            }
            try {
                ProductDTO dto = objectMapper.readValue(cachedJson, new TypeReference<ProductDTO>() {});
                // 1b. 检查是否逻辑过期（热度 + 浏览量用于判断是否需要异步刷新）
                if (isLogicallyExpired(id)) {
                    asyncRefreshCache(id, cacheKey);
                }
                incrViewCount(id);
                return dto;
            } catch (Exception e) {
                stringRedisTemplate.delete(cacheKey);
            }
        }

        // 2. 缓存未命中 → 互斥锁防击穿
        return getProductByIdWithLock(id, cacheKey);
    }

    /**
     * 互斥锁 + 双重检查 + 空值缓存 + 随机 TTL
     */
    private ProductDTO getProductByIdWithLock(Long id, String cacheKey) {
        String lockKey = RedisConstant.cacheLockKey(cacheKey);

        // 2a. 尝试获取互斥锁
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", RedisConstant.CACHE_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            try {
                // 2b. 双重检查：获取锁后再次查缓存
                String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null) {
                    if (RedisConstant.NULL_CACHE_MARKER.equals(cachedJson)) {
                        throw new BusinessException(404, "商品不存在");
                    }
                    try {
                        return objectMapper.readValue(cachedJson, new TypeReference<ProductDTO>() {});
                    } catch (Exception e) {
                        stringRedisTemplate.delete(cacheKey);
                    }
                }

                // 2c. 查数据库
                Product product = super.getById(id);
                if (product == null) {
                    // 防穿透：缓存空值标记（TTL 1 分钟）
                    stringRedisTemplate.opsForValue().set(cacheKey, RedisConstant.NULL_CACHE_MARKER,
                            RedisConstant.NULL_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
                    throw new BusinessException(404, "商品不存在");
                }
                ProductDTO dto = toProductDTO(product);

                // 2d. 防雪崩：TTL 加随机偏移（30 分钟内随机增值 0~5 分钟）
                long ttl = RedisConstant.PRODUCT_DETAIL_TTL_SECONDS
                        + ThreadLocalRandom.current().nextLong(RedisConstant.TTL_RANDOM_MAX_SECONDS);
                try {
                    String json = objectMapper.writeValueAsString(dto);
                    stringRedisTemplate.opsForValue().set(cacheKey, json, ttl, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    // 缓存写入失败不影响主流程
                }

                // 记录逻辑过期时间戳（用于后续逻辑过期判断）
                recordLogicalExpireTime(id);

                incrViewCount(id);
                return dto;
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        } else {
            // 2e. 获取锁失败 → 等待 50ms 后递归重试
            try {
                Thread.sleep(RedisConstant.CACHE_LOCK_RETRY_MS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return getProductById(id); // 递归重试，此时缓存可能已被其他线程重建
        }
    }

    /**
     * 判断缓存是否逻辑过期（依据热度累计差值判断）
     * 热度间隔超过阈值 → 认为需要刷新
     */
    private boolean isLogicallyExpired(Long productId) {
        try {
            String expireKey = RedisConstant.productDetailKey(productId) + ":EXPIRE";
            String expireTime = stringRedisTemplate.opsForValue().get(expireKey);
            if (expireTime == null) return true;
            long expireAt = Long.parseLong(expireTime);
            return System.currentTimeMillis() > expireAt;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 记录逻辑过期时间（当前时间 + 30分钟）
     */
    private void recordLogicalExpireTime(Long productId) {
        try {
            String expireKey = RedisConstant.productDetailKey(productId) + ":EXPIRE";
            long expireAt = System.currentTimeMillis() + RedisConstant.PRODUCT_DETAIL_TTL_SECONDS * 1000;
            stringRedisTemplate.opsForValue().set(expireKey, String.valueOf(expireAt),
                    RedisConstant.PRODUCT_DETAIL_TTL_SECONDS + RedisConstant.TTL_RANDOM_MAX_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // 非关键路径
        }
    }

    /**
     * 异步刷新缓存（逻辑过期时触发，先返回旧数据）
     */
    private void asyncRefreshCache(Long productId, String cacheKey) {
        try {
            String lockKey = RedisConstant.cacheLockKey(cacheKey) + ":REFRESH";
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", RedisConstant.CACHE_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                // 异步刷新
                new Thread(() -> {
                    try {
                        Product product = super.getById(productId);
                        if (product != null) {
                            ProductDTO dto = toProductDTO(product);
                            long ttl = RedisConstant.PRODUCT_DETAIL_TTL_SECONDS
                                    + ThreadLocalRandom.current().nextLong(RedisConstant.TTL_RANDOM_MAX_SECONDS);
                            String json = objectMapper.writeValueAsString(dto);
                            stringRedisTemplate.opsForValue().set(cacheKey, json, ttl, TimeUnit.SECONDS);
                            recordLogicalExpireTime(productId);
                        }
                    } catch (Exception ignored) {
                        // 刷新失败不影响主流程
                    } finally {
                        stringRedisTemplate.delete(lockKey);
                    }
                }).start();
            }
        } catch (Exception ignored) {
            // 非关键路径
        }
    }

    /**
     * 浏览量 Redis 自增计数，同时更新热度排行榜
     */
    private void incrViewCount(Long productId) {
        try {
            stringRedisTemplate.opsForValue().increment(RedisConstant.productViewKey(productId));
            // 同时更新热度排行榜（浏览量 +1 热度）
            stringRedisTemplate.opsForZSet().incrementScore(RedisConstant.PRODUCT_HOT_RANK_KEY, String.valueOf(productId), 1);
        } catch (Exception e) {
            // 浏览量计数失败不影响主流程
        }
    }

    @Override
    public List<ProductDTO> listProducts(Long shopId) {
        // 分类商品列表缓存（仅非店铺筛选时使用）
        if (shopId == null) {
            String cacheKey = RedisConstant.productListCategoryKey(0L);
            try {
                String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null) {
                    return objectMapper.readValue(cachedJson, new TypeReference<List<ProductDTO>>() {});
                }
            } catch (Exception ignored) {}
        }

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(shopId != null, Product::getShopId, shopId);
        wrapper.orderByDesc(Product::getCreateTime);
        List<Product> list = super.list(wrapper);
        List<ProductDTO> result = list.stream().map(this::toProductDTO).collect(Collectors.toList());

        // 写入缓存（防雪崩：TTL 加随机偏移）
        if (shopId == null) {
            try {
                String json = objectMapper.writeValueAsString(result);
                long ttl = RedisConstant.PRODUCT_LIST_TTL_SECONDS
                        + ThreadLocalRandom.current().nextLong(RedisConstant.TTL_RANDOM_MAX_SECONDS);
                stringRedisTemplate.opsForValue().set(RedisConstant.productListCategoryKey(0L), json,
                        ttl, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }

        return result;
    }

    @Override
    public Page<ProductDTO> pageProducts(Integer current, Integer size,
                                         String name, Long categoryId,
                                         Long shopId, Integer status,
                                         Long parentCategoryId) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Product::getName, name);
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId);
        wrapper.eq(shopId != null, Product::getShopId, shopId);
        wrapper.eq(status != null, Product::getStatus, status);
        // 按一级分类查询：收集所有子分类ID
        if (parentCategoryId != null) {
            LambdaQueryWrapper<Category> catWrapper = new LambdaQueryWrapper<>();
            catWrapper.eq(Category::getParentId, parentCategoryId);
            List<Category> subCats = categoryMapper.selectList(catWrapper);
            if (!subCats.isEmpty()) {
                List<Long> subIds = subCats.stream().map(Category::getId).collect(java.util.stream.Collectors.toList());
                wrapper.in(Product::getCategoryId, subIds);
            }
        }
        wrapper.orderByDesc(Product::getCreateTime);
        super.page(page, wrapper);
        return (Page<ProductDTO>) page.convert(this::toProductDTO);
    }

    @Override
    public long countProductsByShop(Long shopId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getShopId, shopId);
        wrapper.eq(Product::getStatus, 1);
        return super.count(wrapper);
    }

    @Override
    public Page<ProductDTO> pageShopProducts(Long shopId, Integer current, Integer size, String keyword, Long categoryId, Integer sort) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getShopId, shopId);
        wrapper.eq(Product::getStatus, 1);
        wrapper.like(StringUtils.hasText(keyword), Product::getName, keyword);
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId);

        // sort: 0=综合(default), 1=价格升序, 2=价格降序, 3=新品优先
        if (sort == null || sort == 0) {
            wrapper.orderByDesc(Product::getCreateTime);
        } else if (sort == 1) {
            wrapper.orderByAsc(Product::getPrice);
        } else if (sort == 2) {
            wrapper.orderByDesc(Product::getPrice);
        } else if (sort == 3) {
            wrapper.orderByDesc(Product::getCreateTime);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        super.page(page, wrapper);
        return (Page<ProductDTO>) page.convert(this::toProductDTO);
    }

    /**
     * 个性化推荐：综合评分 = 销量×70% + 热度×30%
     */
    @Override
    public Page<ProductDTO> recommendProducts(Integer current, Integer size, Long categoryId) {
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId);
        wrapper.last("ORDER BY (sales * 0.7 + view_point * 0.3) DESC, create_time DESC");
        super.page(page, wrapper);
        return (Page<ProductDTO>) page.convert(this::toProductDTO);
    }

    // ==================== 缓存辅助方法 ====================

    /**
     * 删除商品详情 Redis 缓存（同时清除逻辑过期标记）
     */
    private void deleteProductCache(Long productId) {
        try {
            stringRedisTemplate.delete(RedisConstant.productDetailKey(productId));
            stringRedisTemplate.delete(RedisConstant.productDetailKey(productId) + ":EXPIRE");
        } catch (Exception e) {
            // 缓存删除失败不影响主流程
        }
    }

    /** 清除商品列表缓存，使用户端立即看到新增/变更的商品 */
    private void clearProductListCache() {
        try {
            stringRedisTemplate.delete(RedisConstant.productListCategoryKey(0L));
        } catch (Exception e) {
            // 缓存清除失败不影响主流程
        }
    }

    /**
     * 校验当前商家是否有权操作该商品
     * SUPER_ADMIN/ADMIN 可以操作全部商品
     * MERCHANT 只能操作自己店铺的商品
     */
    private void checkProductOwnership(Long productId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        // 超级管理员和管理员可以管理全部商品
        if ("SUPER_ADMIN".equals(user.getRoleCode()) || "ADMIN".equals(user.getRoleCode())) {
            return;
        }
        // 商家只能操作自己店铺的商品
        if ("MERCHANT".equals(user.getRoleCode())) {
            Product product = super.getById(productId);
            if (product == null) {
                throw new BusinessException(404, "商品不存在");
            }
            Long shopId = getCurrentMerchantShopId();
            if (shopId == null || !shopId.equals(product.getShopId())) {
                throw new BusinessException(403, "无权操作其他店铺的商品");
            }
            return;
        }
        throw new BusinessException(403, "无权限操作商品");
    }

    // ==================== DTO 转换 ====================

    /**
     * Product 转 ProductDTO（关联分类、店铺通过 JOIN 查询一次性获取）
     * 轮播图和 SKU 仍为独立查询（一对多关系）
     */
    private ProductDTO toProductDTO(Product product) {
        // 一次 JOIN 查询获取分类名称、店铺名称、库存
        ProductDTO dto = baseMapper.selectProductDetail(product.getId());
        if (dto == null) {
            dto = new ProductDTO();
            BeanUtils.copyProperties(product, dto);
        }

        if (product.getCreateTime() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dto.setCreateTime(sdf.format(product.getCreateTime()));
        }

        // 查询轮播图列表
        LambdaQueryWrapper<ProductImage> imgWrapper = new LambdaQueryWrapper<>();
        imgWrapper.eq(ProductImage::getProductId, product.getId());
        List<ProductImage> images = productImageMapper.selectList(imgWrapper);
        dto.setImageList(images.stream().map(img -> {
            ProductImageDTO imgDTO = new ProductImageDTO();
            BeanUtils.copyProperties(img, imgDTO);
            return imgDTO;
        }).collect(Collectors.toList()));

        // 查询 SKU 列表
        LambdaQueryWrapper<ProductSku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.eq(ProductSku::getProductId, product.getId());
        List<ProductSku> skus = productSkuMapper.selectList(skuWrapper);
        dto.setSkuList(skus.stream().map(sku -> {
            ProductSkuDTO skuDTO = new ProductSkuDTO();
            BeanUtils.copyProperties(sku, skuDTO);
            return skuDTO;
        }).collect(Collectors.toList()));

        return dto;
    }

    // ==================== 热门商品排行榜 ZSet ====================

    /**
     * 获取热门商品（按销量排序，从 Redis ZSet 读取）
     * @param limit 返回条数
     */
    public List<Long> getHotProductIds(int limit) {
        Set<String> topIds = stringRedisTemplate.opsForZSet().reverseRange(RedisConstant.PRODUCT_HOT_RANK_KEY, 0, limit - 1);
        if (topIds != null && !topIds.isEmpty()) {
            return topIds.stream().map(Long::parseLong).collect(Collectors.toList());
        }

        // ZSet 为空，从数据库回填种子数据（纯热度）
        try {
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getStatus, 1)
                   .last("ORDER BY view_point DESC LIMIT " + Math.min(limit * 2, 100));
            List<Product> topProducts = super.list(wrapper);
            if (topProducts != null && !topProducts.isEmpty()) {
                for (Product p : topProducts) {
                    double score = p.getViewPoint() != null ? p.getViewPoint() : 0;
                    stringRedisTemplate.opsForZSet().add(RedisConstant.PRODUCT_HOT_RANK_KEY, String.valueOf(p.getId()), score);
                }
                // 重新查询
                topIds = stringRedisTemplate.opsForZSet().reverseRange(RedisConstant.PRODUCT_HOT_RANK_KEY, 0, limit - 1);
                if (topIds != null && !topIds.isEmpty()) {
                    return topIds.stream().map(Long::parseLong).collect(Collectors.toList());
                }
            }
        } catch (Exception ignored) {}

        return new ArrayList<>();
    }

    /**
     * 更新商品销量到排行榜（下单后调用）
     */
    public void incrementHotScore(Long productId, int delta) {
        try {
            stringRedisTemplate.opsForZSet().incrementScore(RedisConstant.PRODUCT_HOT_RANK_KEY, String.valueOf(productId), delta);
        } catch (Exception ignored) {}
    }

    // ==================== 库存分布式锁（防超卖） ====================

    /**
     * 尝试获取 SKU 库存锁（下单前调用）
     * @param skuId SKU ID
     * @return 是否获取成功
     */
    public boolean tryLockStock(Long skuId) {
        String key = RedisConstant.stockLockKey(skuId);
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, "1",
                RedisConstant.STOCK_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放 SKU 库存锁（下单完成后调用）
     * @param skuId SKU ID
     */
    public void unlockStock(Long skuId) {
        stringRedisTemplate.delete(RedisConstant.stockLockKey(skuId));
    }

    // ==================== 浏览量批量同步 ====================

    /**
     * 将 Redis 中所有商品浏览量同步到数据库，定时任务调用
     * 实际项目可改用 RabbitMQ 异步批量处理
     */
    public void syncViewCountToDB() {
        // 这里简化处理：遍历所有商品，从 Redis 读取累加后写入数据库
        // 完整方案：使用 Redis KEYS 或 SCAN 遍历，批量更新
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Product::getId);
        List<Product> all = baseMapper.selectList(wrapper);
        for (Product p : all) {
            Long viewCount = stringRedisTemplate.opsForValue().increment(RedisConstant.productViewKey(p.getId()), 0);
            if (viewCount != null && viewCount > 0) {
                Product update = new Product();
                update.setId(p.getId());
                update.setViewPoint(viewCount.intValue());
                baseMapper.updateById(update);
            }
        }
    }

    @Override
    public void initStockCache(Long skuId, Integer stock) {
        stringRedisTemplate.opsForValue().set(
                RedisConstant.stockSkuKey(skuId), String.valueOf(stock));
    }
}