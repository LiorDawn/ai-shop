package org.example.aishop.service.product;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.aishop.dto.ProductDTO;
import org.example.aishop.entity.product.Product;

import java.util.List;

public interface ProductService extends IService<Product> {

    /**
     * 获取当前登录商家对应的店铺ID
     */
    Long getCurrentMerchantShopId();

    void addProduct(ProductDTO productDTO);

    void updateProduct(ProductDTO productDTO);

    void deleteProduct(Long id);

    void deleteBatchProducts(List<Long> ids);

    void upProduct(Long id);

    void downProduct(Long id);

    ProductDTO getProductById(Long id);

    List<ProductDTO> listProducts(Long shopId);

    IPage<ProductDTO> pageProducts(Integer current, Integer size, String name, Long categoryId, Long shopId, Integer status, Long parentCategoryId);

    /** 统计店铺商品数（仅上架） */
    long countProductsByShop(Long shopId);

    /** 店铺内商品搜索（支持排序） */
    Page<ProductDTO> pageShopProducts(Long shopId, Integer current, Integer size, String keyword, Long categoryId, Integer sort);

    /** 个性化推荐（销量×新鲜度混合评分） */
    Page<ProductDTO> recommendProducts(Integer current, Integer size, Long categoryId);

    /** 获取热门商品ID列表（按销量排序，Redis ZSet） */
    List<Long> getHotProductIds(int limit);

    /** 更新商品销量到排行榜 */
    void incrementHotScore(Long productId, int delta);

    /** 尝试获取 SKU 库存锁 */
    boolean tryLockStock(Long skuId);

    /** 释放 SKU 库存锁 */
    void unlockStock(Long skuId);

    /** 浏览量同步到数据库 */
    void syncViewCountToDB();

    /** 初始化 SKU 库存到 Redis 缓存 */
    void initStockCache(Long skuId, Integer stock);
}