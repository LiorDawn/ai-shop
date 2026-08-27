package org.example.aishop.controller.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.service.ai.AIContentService;
import org.example.aishop.dto.ProductDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.service.product.ProductService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "商品管理", description = "商品CRUD、分页查询、热门推荐、个性化推荐")
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private AIContentService aiContentService;

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<Void> add(@RequestBody ProductDTO productDTO) {
        productService.addProduct(productDTO);
        return Result.success("添加成功");
    }

    @Operation(summary = "修改商品")
    @PutMapping
    public Result<Void> update(@RequestBody ProductDTO productDTO) {
        productService.updateProduct(productDTO);
        return Result.success("修改成功");
    }

    @Operation(summary = "删除商品", description = "同时删除 SKU 和轮播图")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success("删除成功");
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        productService.deleteBatchProducts(ids);
        return Result.success("批量删除成功");
    }

    /**
     * 商品上架
     */
    @PutMapping("/up/{id}")
    public Result<Void> up(@PathVariable Long id) {
        productService.upProduct(id);
        return Result.success("商品已上架");
    }

    @Operation(summary = "商品下架")
    @PutMapping("/down/{id}")
    public Result<Void> down(@PathVariable Long id) {
        productService.downProduct(id);
        return Result.success("商品已下架");
    }

    @Operation(summary = "商品详情", description = "含主图、轮播图、SKU 信息")
    @GetMapping("/{id}")
    public Result<ProductDTO> getById(@PathVariable Long id) {
        ProductDTO dto = productService.getProductById(id);
        return Result.success("查询成功", dto);
    }

    @Operation(summary = "商品列表", description = "商家自动过滤所属店铺")
    @GetMapping("/list")
    public Result<List<ProductDTO>> list(@RequestParam(required = false) Long shopId) {
        // 商家自动注入所属店铺ID
        if (shopId == null) {
            UserDTO user = UserHolder.getUser();
            if (user != null && "MERCHANT".equals(user.getRoleCode())) {
                shopId = productService.getCurrentMerchantShopId();
            }
        }
        List<ProductDTO> list = productService.listProducts(shopId);
        return Result.success("查询成功", list);
    }

    @Operation(summary = "商品分页查询", description = "支持按名称、分类、店铺、上下架状态筛选")
    @GetMapping("/page")
    public Result<Page<ProductDTO>> page(@RequestParam(defaultValue = "1") Integer current,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) Long shopId,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) Long parentCategoryId) {
        // 商家自动注入所属店铺ID
        if (shopId == null) {
            UserDTO user = UserHolder.getUser();
            if (user != null && "MERCHANT".equals(user.getRoleCode())) {
                shopId = productService.getCurrentMerchantShopId();
            }
        }
        Page<ProductDTO> page = (Page<ProductDTO>) productService.pageProducts(current, size, name, categoryId, shopId, status, parentCategoryId);
        return Result.success("查询成功", page);
    }

    @Operation(summary = "热门商品推荐", description = "基于 Redis ZSet 热度排行榜（浏览量+销量混合评分）")
    @GetMapping("/hot")
    public Result<List<ProductDTO>> hot(@RequestParam(defaultValue = "10") int limit) {
        List<Long> hotIds = productService.getHotProductIds(limit);
        if (hotIds.isEmpty()) {
            return Result.success("查询成功", new ArrayList<>());
        }
        // 批量查询商品详情
        List<ProductDTO> list = hotIds.stream()
                .map(productService::getProductById)
                .collect(Collectors.toList());
        return Result.success("查询成功", list);
    }

    @Operation(summary = "个性化推荐", description = "销量×70% + 热度×30% 混合评分，优先展示高热度商品")
    @GetMapping("/recommend")
    public Result<Page<ProductDTO>> recommend(@RequestParam(defaultValue = "1") Integer current,
                                              @RequestParam(defaultValue = "12") Integer size,
                                              @RequestParam(required = false) Long categoryId) {
        Page<ProductDTO> page = productService.recommendProducts(current, size, categoryId);
        return Result.success("查询成功", page);
    }

    @Operation(summary = "AI 生成商品描述", description = "根据商品名称和分类，AI 自动生成商品描述和卖点文案")
    @GetMapping("/ai-describe")
    public Result<String> aiDescribe(@RequestParam String name,
                                     @RequestParam(required = false) String categoryName,
                                     @RequestParam(required = false) String imageUrl) {
        String description = aiContentService.generateProductDescription(name, categoryName, imageUrl);
        if (description == null) {
            return Result.fail("AI 生成失败，请稍后重试");
        }
        return Result.success("生成成功", description);
    }
}