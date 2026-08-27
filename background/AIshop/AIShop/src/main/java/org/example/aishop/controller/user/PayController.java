package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.pay.PayService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private PayService payService;

    /**
     * 支付宝下单接口
     * 返回自动提交的 HTML 支付表单
     */
    @RepeatSubmit(prefix = "repeat:submit:pay:", leaseTime = 5, message = "支付正在处理中，请勿重复提交")
    @PostMapping("/create/{orderId}")
    public Result<String> createPay(@PathVariable Long orderId) {
        Long userId = UserHolder.getUserId();
        String form = payService.createPayForm(orderId, userId);
        return Result.success("生成支付表单成功", form);
    }

    @Operation(summary = "支付宝异步通知", description = "支付宝 POST 回调，更新订单支付状态（需外网可访问）")
    @PostMapping("/notify")
    public String notify(@RequestParam Map<String, String> params) {
        return payService.handleNotify(params);
    }

    /**
     * 模拟支付成功（开发环境用）
     * 跳过支付宝异步通知，直接更新订单状态为已支付、待发货
     */
    @PostMapping("/simulate/{orderId}")
    public Result<Void> simulatePay(@PathVariable Long orderId) {
        Long userId = UserHolder.getUserId();
        payService.simulatePay(orderId, userId);
        return Result.success("模拟支付成功");
    }

    @Operation(summary = "查询支付状态", description = "主动调用支付宝查询 API，确认支付结果并更新订单状态")
    @PostMapping("/query/{orderId}")
    public Result<Boolean> queryPay(@PathVariable Long orderId) {
        Long userId = UserHolder.getUserId();
        boolean paid = payService.queryPayStatus(orderId, userId);
        return Result.success(paid ? "支付成功" : "尚未支付", paid);
    }
}