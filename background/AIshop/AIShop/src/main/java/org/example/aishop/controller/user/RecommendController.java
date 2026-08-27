package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.dto.ProductDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.product.RecommendService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品推荐", description = "猜你喜欢、看了又看、买了又买、同类推荐")
@RestController
@RequestMapping("/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @Operation(summary = "猜你喜欢", description = "基于用户行为协同过滤 + 热门补充，未登录用户返回热门商品")
    @GetMapping("/guess")
    public Result<List<ProductDTO>> guess(@RequestParam(defaultValue = "12") int limit) {
        Long userId = UserHolder.getUserId();
        // 未登录用户也返回热门商品推荐，而非直接报错
        return recommendService.guessYouLike(userId, limit);
    }

    @Operation(summary = "看了又看", description = "同类商品推荐")
    @GetMapping("/also-viewed/{productId}")
    public Result<List<ProductDTO>> alsoViewed(@PathVariable Long productId,
                                                @RequestParam(defaultValue = "8") int limit) {
        return recommendService.alsoViewed(productId, limit);
    }

    @Operation(summary = "买了又买", description = "购买该商品的用户还买了什么")
    @GetMapping("/also-bought/{productId}")
    public Result<List<ProductDTO>> alsoBought(@PathVariable Long productId,
                                                @RequestParam(defaultValue = "8") int limit) {
        return recommendService.alsoBought(productId, limit);
    }

    @Operation(summary = "同类商品", description = "基于内容的同类商品推荐")
    @GetMapping("/similar/{productId}")
    public Result<List<ProductDTO>> similar(@PathVariable Long productId,
                                             @RequestParam(defaultValue = "8") int limit) {
        return recommendService.similarProducts(productId, limit);
    }
}