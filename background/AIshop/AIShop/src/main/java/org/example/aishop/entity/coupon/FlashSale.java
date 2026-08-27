package org.example.aishop.entity.coupon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动商品
 */
@Data
@TableName("flash_sale")
public class FlashSale {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 秒杀价格 */
    private BigDecimal flashPrice;

    /** 秒杀库存 */
    private Integer stock;

    /** 秒杀开始时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    /** 状态：0-未开始 1-进行中 2-已结束 */
    private Integer status;

    /** 每人限购数量 */
    private Integer limitPerUser;

    /** 动态签名密钥（MD5 加密） */
    private String signKey;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}