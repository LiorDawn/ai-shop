package org.example.aishop.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 销量同步 MQ 消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesSyncMQMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 产品ID */
    private Long productId;
    /** 增量销量 */
    private Integer delta;
}