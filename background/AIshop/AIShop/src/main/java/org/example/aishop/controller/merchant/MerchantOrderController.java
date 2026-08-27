package org.example.aishop.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import org.example.aishop.dto.OrderDTO;
import org.example.aishop.dto.OrderDetailDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.service.order.OrderService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/merchant/order")
public class MerchantOrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderMapper orderMapper;

    @Operation(summary = "商家订单列表", description = "分页查询当前店铺订单")
    @GetMapping("/page")
    public Result<Page<OrderDTO>> page(@RequestParam(defaultValue = "1") Integer current,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) String orderNo,
                                       @RequestParam(required = false) Integer orderStatus) {
        Long shopId = UserHolder.getShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        Page<OrderDTO> page = orderService.pageOrders(current, size, orderNo, orderStatus, shopId, null, null);
        return Result.success("查询成功", page);
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<OrderDetailDTO> detail(@PathVariable Long id) {
        Long shopId = UserHolder.getShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        OrderDetailDTO dto = orderService.getOrderDetail(id);
        // 校验该订单是否属于当前店铺
        boolean belongs = dto.getItems().stream().anyMatch(item -> item.getShopId().equals(shopId));
        if (!belongs) {
            throw new BusinessException(403, "无权查看该订单");
        }
        return Result.success("查询成功", dto);
    }

    @Operation(summary = "商家发货")
    @PutMapping("/deliver/{id}")
    public Result<Void> deliver(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long shopId = UserHolder.getShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        String logistics = body.get("logistics");
        if (logistics == null || logistics.trim().isEmpty()) {
            throw new BusinessException(400, "物流单号不能为空");
        }
        // 校验订单属于当前店铺
        OrderDetailDTO dto = orderService.getOrderDetail(id);
        boolean belongs = dto.getItems().stream().anyMatch(item -> item.getShopId().equals(shopId));
        if (!belongs) {
            throw new BusinessException(403, "无权操作该订单");
        }
        orderService.deliverOrder(id, logistics);
        return Result.success("发货成功");
    }

    /** 添加订单备注 */
    @PutMapping("/remark/{id}")
    public Result<Void> remark(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long shopId = UserHolder.getShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        String remark = body.get("remark");
        // 校验订单属于当前店铺
        OrderDetailDTO dto = orderService.getOrderDetail(id);
        boolean belongs = dto.getItems().stream().anyMatch(item -> item.getShopId().equals(shopId));
        if (!belongs) {
            throw new BusinessException(403, "无权操作该订单");
        }
        // 备注保存在order表的remark字段
        Orders order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        order.setRemark(remark);
        orderMapper.updateById(order);
        return Result.success("备注已保存");
    }
}