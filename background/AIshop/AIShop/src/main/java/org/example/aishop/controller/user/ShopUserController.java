package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.common.result.Result;
import org.example.aishop.dto.*;
import org.example.aishop.entity.product.Category;
import org.example.aishop.entity.merchant.ShopFollow;
import org.example.aishop.mapper.merchant.ShopFollowMapper;
import org.example.aishop.service.merchant.ShopFollowService;
import org.example.aishop.service.merchant.ShopService;
import org.example.aishop.service.product.CategoryService;
import org.example.aishop.service.product.ProductService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "用户端店铺", description = "店铺信息浏览、关注/取消关注、店铺内商品查看")
@RestController
@RequestMapping("/shop")
public class ShopUserController {

    @Autowired
    private ShopFollowService shopFollowService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopFollowMapper shopFollowMapper;

    @Operation(summary = "获取关注店铺列表")
    @GetMapping("/follows")
    public Result<List<ShopDTO>> follows() {
        Long userId = UserHolder.getUserId();
        LambdaQueryWrapper<ShopFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopFollow::getUserId, userId)
               .orderByDesc(ShopFollow::getCreateTime);
        List<ShopFollow> list = shopFollowMapper.selectList(wrapper);
        List<ShopDTO> dtoList = list.stream()
                .map(sf -> shopService.getShopById(sf.getShopId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return Result.success("查询成功", dtoList);
    }

    @Operation(summary = "店铺详情", description = "含商品数、关注数、是否已关注")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        ShopDTO shopDTO = shopService.getShopById(id);
        long productCount = productService.countProductsByShop(id);
        long followerCount = shopFollowService.countFollowers(id);

        Map<String, Object> result = new HashMap<>();
        result.put("shop", shopDTO);
        result.put("productCount", productCount);
        result.put("followerCount", followerCount);

        // 是否已关注
        UserDTO user = UserHolder.getUser();
        if (user != null) {
            result.put("followed", shopFollowService.isFollowed(id));
        } else {
            result.put("followed", false);
        }

        return Result.success("查询成功", result);
    }

    @Operation(summary = "关注店铺")
    @PostMapping("/follow/{id}")
    public Result<Void> follow(@PathVariable Long id) {
        shopFollowService.follow(id);
        return Result.success("关注成功");
    }

    /**
     * 取消关注
     */
    @DeleteMapping("/follow/{id}")
    public Result<Void> unfollow(@PathVariable Long id) {
        shopFollowService.unfollow(id);
        return Result.success("已取消关注");
    }

    /**
     * 检查是否已关注
     */
    @GetMapping("/follow/check/{id}")
    public Result<Boolean> checkFollow(@PathVariable Long id) {
        return Result.success("查询成功", shopFollowService.isFollowed(id));
    }

    /**
     * 店铺内商品分类
     */
    @GetMapping("/{id}/categories")
    public Result<List<Category>> categories(@PathVariable Long id) {
        // 获取该店铺所有上架商品使用的分类
        List<ProductDTO> products = productService.listProducts(id);
        Set<Long> usedCategoryIds = products.stream()
                .map(ProductDTO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (usedCategoryIds.isEmpty()) {
            return Result.success("查询成功", Collections.emptyList());
        }

        List<Category> allCategories = categoryService.listCategories();
        List<Category> shopCategories = allCategories.stream()
                .filter(c -> usedCategoryIds.contains(c.getId()))
                .collect(Collectors.toList());

        return Result.success("查询成功", shopCategories);
    }

    @Operation(summary = "店铺内商品搜索", description = "分页 + 排序（0=综合 1=价格升序 2=价格降序 3=新品）")
    @GetMapping("/{id}/products")
    public Result<Page<ProductDTO>> products(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") Integer sort) {
        // sort: 0=综合, 1=价格升序, 2=价格降序, 3=新品优先
        Page<ProductDTO> page = productService.pageShopProducts(id, current, size, keyword, categoryId, sort);
        return Result.success("查询成功", page);
    }
}