package org.example.aishop.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建 MQ 消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateMQMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long orderId;
    private Long userId;
    private String orderNo;
    private BigDecimal totalPrice;
    private BigDecimal couponPrice;
    private BigDecimal actualPrice;
    private Long addressId;
    private Long couponId;
    private String remark;
    private List<OrderItemEntry> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long productId;
        private Long skuId;
        private Long shopId;
        private Integer num;
        private BigDecimal price;
    }
}