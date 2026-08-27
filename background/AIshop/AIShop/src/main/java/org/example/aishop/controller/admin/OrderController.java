package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.OrderDTO;
import org.example.aishop.dto.OrderDetailDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "管理端订单管理", description = "全平台订单查询、发货、取消")
@RestController
@RequestMapping("/admin/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/page")
    public Result<Page<OrderDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        Page<OrderDTO> page = orderService.pageOrders(current, size, orderNo, orderStatus, shopId, startTime, endTime);
        return Result.success("查询成功", page);
    }

    @GetMapping("/{id}")
    public Result<OrderDetailDTO> detail(@PathVariable Long id) {
        OrderDetailDTO detail = orderService.getOrderDetail(id);
        return Result.success("查询成功", detail);
    }

    @PutMapping("/deliver/{id}")
    public Result<Void> deliver(@PathVariable Long id, @RequestParam String logistics) {
        orderService.deliverOrder(id, logistics);
        return Result.success("发货成功");
    }

    @Operation(summary = "管理端取消订单")
    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success("取消成功");
    }

    @PutMapping("/complete/{id}")
    public Result<Void> complete(@PathVariable Long id) {
        orderService.completeOrder(id);
        return Result.success("操作成功");
    }
}