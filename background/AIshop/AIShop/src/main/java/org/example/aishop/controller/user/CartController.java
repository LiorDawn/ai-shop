package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.dto.CartDTO;
import org.example.aishop.dto.CartItemVO;
import org.example.aishop.dto.CartSettleVO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.order.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "购物车", description = "加入购物车、商品列表、数量修改、选中结算")
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Operation(summary = "加入购物车")
    @RepeatSubmit(prefix = "repeat:submit:cart:add:", leaseTime = 2, message = "正在添加中，请稍后再试")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody CartDTO cartDTO) {
        cartService.add(cartDTO.getProductId(), cartDTO.getSkuId(), cartDTO.getNum());
        return Result.success("已加入购物车");
    }

    @Operation(summary = "购物车列表")
    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        List<CartItemVO> list = cartService.listCart();
        return Result.success("查询成功", list);
    }

    @Operation(summary = "修改数量")
    @PutMapping("/{id}/num")
    public Result<Void> updateNum(@PathVariable Long id, @RequestParam Integer num) {
        cartService.updateNum(id, num);
        return Result.success();
    }

    /** 选中/取消选中 */
    @PutMapping("/{id}/check")
    public Result<Void> toggleCheck(@PathVariable Long id, @RequestParam Integer checked) {
        cartService.toggleCheck(id, checked);
        return Result.success();
    }

    /** 全选/全不选 */
    @PutMapping("/check-all")
    public Result<Void> checkAll(@RequestParam Integer checked) {
        cartService.checkAll(checked);
        return Result.success();
    }

    /** 删除单个 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量删除购物车商品")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        cartService.deleteBatch(ids);
        return Result.success();
    }

    @Operation(summary = "结算前置校验", description = "校验库存/金额并返回结算摘要")
    @PostMapping("/settle-check")
    public Result<CartSettleVO> settleCheck() {
        CartSettleVO settleVO = cartService.settleCheck();
        return Result.success(settleVO);
    }
}