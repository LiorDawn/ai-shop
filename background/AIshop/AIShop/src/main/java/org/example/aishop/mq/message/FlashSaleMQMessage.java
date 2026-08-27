package org.example.aishop.mq.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 秒杀下单消息
 */
@Data
public class FlashSaleMQMessage implements Serializable {

    private Long flashSaleId;
    private Long productId;
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private String orderNo;
    private String requestId; // 用于轮询结果
}