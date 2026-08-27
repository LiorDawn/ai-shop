package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.dto.OrderDTO;
import org.example.aishop.dto.OrderDetailDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.order.OrderService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户订单", description = "订单创建、订单列表、取消、确认收货")
@RestController
@RequestMapping("/order")
public class UserOrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "我的订单列表")
    @GetMapping("/page")
    public Result<Page<OrderDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer orderStatus) {
        Long userId = UserHolder.getUserId();
        Page<OrderDTO> page = orderService.pageMyOrders(userId, current, size, orderNo, orderStatus);
        return Result.success("查询成功", page);
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<OrderDetailDTO> detail(@PathVariable Long id) {
        OrderDetailDTO detail = orderService.getOrderDetail(id);
        return Result.success("查询成功", detail);
    }

    /** 取消订单 */
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success("取消成功");
    }

    @Operation(summary = "确认收货", description = "更新销量和热度排行榜")
    @PutMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Long id) {
        Long userId = UserHolder.getUserId();
        orderService.confirmReceive(id, userId);
        return Result.success("确认收货成功");
    }

    /** 创建订单（从购物车结算） */
    @RepeatSubmit(prefix = "repeat:submit:order:create:", leaseTime = 5, message = "订单正在处理中，请勿重复提交")
    @PostMapping("/create")
    public Result<Long> create(
            @RequestParam Long addressId,
            @RequestParam String productIds,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) Long couponId) {
        Long userId = UserHolder.getUserId();
        Long orderId = orderService.createOrder(userId, addressId, productIds, remark, couponId);
        return Result.success("下单成功", orderId);
    }
}