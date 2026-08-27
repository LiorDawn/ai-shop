package org.example.aishop.service.order;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.OrderDTO;
import org.example.aishop.dto.OrderDetailDTO;

import java.util.Date;

public interface OrderService {
    /**
     * 分页查询订单列表
     */
    Page<OrderDTO> pageOrders(Integer current, Integer size,
                              String orderNo, Integer orderStatus,
                              Long shopId, Date startTime, Date endTime);

    /**
     * 获取订单详情
     */
    OrderDetailDTO getOrderDetail(Long orderId);

    /**
     * 发货（填写物流单号，状态变为待收货）
     */
    void deliverOrder(Long orderId, String logistics);

    /**
     * 取消订单（仅待付款状态可取消）
     */
    void cancelOrder(Long orderId);

    /**
     * 完成订单（仅待收货状态可完成）
     */
    void completeOrder(Long orderId);

    /**
     * 用户端：分页查询我的订单
     */
    Page<OrderDTO> pageMyOrders(Long userId, Integer current, Integer size,
                                String orderNo, Integer orderStatus);

    /**
     * 用户端：创建订单（从购物车结算）
     * @param productIds 逗号分隔的商品ID列表
     * @param couponId 优惠券ID（可选）
     */
    Long createOrder(Long userId, Long addressId, String productIds, String remark, Long couponId);

    /**
     * 用户端：确认收货
     */
    void confirmReceive(Long orderId, Long userId);
}