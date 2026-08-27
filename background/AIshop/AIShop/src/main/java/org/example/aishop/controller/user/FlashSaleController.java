package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.coupon.FlashSaleService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "秒杀专区", description = "秒杀活动列表、详情、抢购、结果轮询")
@RestController
@RequestMapping("/flash-sale")
public class FlashSaleController {

    @Autowired
    private FlashSaleService flashSaleService;

    @Operation(summary = "秒杀活动列表")
    @GetMapping("/list")
    public Result<?> list() {
        return flashSaleService.listActive();
    }

    @Operation(summary = "秒杀商品详情")
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return flashSaleService.detail(id);
    }

    @Operation(summary = "获取秒杀地址（签名验证）")
    @PostMapping("/url/{id}")
    public Result<?> getUrl(@PathVariable Long id,
                            @RequestParam String verifyCode,
                            @RequestParam String sign) {
        return flashSaleService.getSeckillUrl(id, verifyCode, sign);
    }

    @Operation(summary = "执行秒杀")
    @PostMapping("/execute/{id}")
    public Result<?> execute(@PathVariable Long id, @RequestParam String token) {
        Long userId = UserHolder.getUserId();
        if (userId == null) return Result.fail("请先登录");
        return flashSaleService.executeSeckill(id, token, userId);
    }

    @Operation(summary = "轮询秒杀结果")
    @GetMapping("/result/{requestId}")
    public Result<?> result(@PathVariable String requestId) {
        return flashSaleService.pollResult(requestId);
    }
}