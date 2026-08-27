package org.example.aishop.service.product;

import org.example.aishop.dto.ProductDTO;
import org.example.aishop.common.result.Result;

import java.util.List;

public interface RecommendService {

    /**
     * 首页「猜你喜欢」- 基于用户行为协同过滤 + 内容推荐
     */
    Result<List<ProductDTO>> guessYouLike(Long userId, int limit);

    /**
     * 商品详情页「看了又看」- 浏览过该商品的用户还浏览了
     */
    Result<List<ProductDTO>> alsoViewed(Long productId, int limit);

    /**
     * 商品详情页「买了又买」- 购买过该商品的用户还买了
     */
    Result<List<ProductDTO>> alsoBought(Long productId, int limit);

    /**
     * 同类商品推荐 - 基于内容的推荐
     */
    Result<List<ProductDTO>> similarProducts(Long productId, int limit);
}