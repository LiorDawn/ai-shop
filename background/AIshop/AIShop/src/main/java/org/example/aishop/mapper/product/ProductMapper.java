package org.example.aishop.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.aishop.dto.ProductDTO;
import org.example.aishop.entity.product.Product;

import java.util.List;
import java.util.Set;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /** 商品详情（关联分类、店铺、子查询库存） */
    ProductDTO selectProductDetail(@Param("id") Long id);

    /** 商品列表（关联分类、店铺），支持多条件筛选 */
    List<ProductDTO> selectProductList(@Param("shopId") Long shopId,
                                       @Param("status") Integer status,
                                       @Param("categoryId") Long categoryId,
                                       @Param("categoryIds") Set<Long> categoryIds,
                                       @Param("name") String name,
                                       @Param("sort") Integer sort);
}