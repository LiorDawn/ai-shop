package org.example.aishop.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.aishop.dto.OrderDTO;
import org.example.aishop.dto.OrderDetailDTO;
import org.example.aishop.entity.order.Orders;

import java.util.Date;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /** 订单列表（关联用户、子查询店铺名称） */
    Page<OrderDTO> selectOrderPage(Page<OrderDTO> page,
                                   @Param("orderNo") String orderNo,
                                   @Param("orderStatus") Integer orderStatus,
                                   @Param("startTime") Date startTime,
                                   @Param("endTime") Date endTime,
                                   @Param("userId") Long userId,
                                   @Param("shopId") Long shopId);

    /** 订单详情（关联用户、地址） */
    OrderDetailDTO selectOrderDetail(@Param("id") Long id);

    /** 订单明细（关联商品、SKU、店铺、售后、评论） */
    List<OrderDetailDTO.OrderItemDTO> selectOrderItems(@Param("orderId") Long orderId);
}