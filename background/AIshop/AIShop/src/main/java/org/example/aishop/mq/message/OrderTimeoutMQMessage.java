package org.example.aishop.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单超时关闭 MQ 消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMQMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long orderId;
    private Long userId;
    /** 快照：下单时扣减的 SKU 列表，用于回滚库存 */
    private String stockSnapshot; // JSON: [{"skuId":1,"num":2},{"skuId":2,"num":1}]
    /** 快照：下单时使用的优惠券ID */
    private Long couponId;
}