package org.example.aishop.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.aishop.dto.StatsDTO;
import org.example.aishop.entity.order.OrderItem;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /** 商品销量排行（TOP 10）- 仅统计已支付订单 */
    @Select("SELECT oi.product_id AS productId, p.name AS productName, p.image AS productImage, " +
            "SUM(oi.num) AS totalSales " +
            "FROM order_item oi " +
            "LEFT JOIN product p ON oi.product_id = p.id " +
            "LEFT JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.pay_status = 1 " +
            "GROUP BY oi.product_id, p.name, p.image " +
            "ORDER BY totalSales DESC LIMIT 10")
    List<StatsDTO.ProductSalesRankVO> selectProductSalesRank();
}