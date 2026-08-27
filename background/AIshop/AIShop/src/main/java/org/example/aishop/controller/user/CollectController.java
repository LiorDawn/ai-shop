package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.product.CollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "收藏管理", description = "商品收藏/取消收藏、收藏状态查询")
@RestController
@RequestMapping("/collect")
public class CollectController {

    @Autowired
    private CollectService collectService;

    @Operation(summary = "收藏商品")
    @RepeatSubmit(prefix = "repeat:submit:collect:", leaseTime = 2, message = "正在收藏中，请稍后再试")
    @PostMapping("/add/{productId}")
    public Result<Void> add(@PathVariable Long productId) {
        collectService.addCollect(productId);
        return Result.success("收藏成功");
    }

    /** 取消收藏 */
    @DeleteMapping("/remove/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        collectService.removeCollect(productId);
        return Result.success("已取消收藏");
    }

    /** 检查是否已收藏 */
    @GetMapping("/check/{productId}")
    public Result<Boolean> check(@PathVariable Long productId) {
        boolean collected = collectService.isCollected(productId);
        return Result.success("查询成功", collected);
    }

    @Operation(summary = "收藏商品ID列表")
    @GetMapping("/ids")
    public Result<List<Long>> collectedIds() {
        List<Long> ids = collectService.listCollectedProductIds();
        return Result.success("查询成功", ids);
    }
}