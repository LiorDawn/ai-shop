package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(description = "购物车商品项")
public class CartItemVO {
    @Schema(description = "购物车记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "规格描述")
    private String spec;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "单价")
    private BigDecimal price;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "数量")
    private Integer num;

    @Schema(description = "是否选中：0=否，1=是")
    private Integer checked;

    @Schema(description = "商品状态：1=上架，0=下架")
    private Integer productStatus;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "创建时间")
    private Date createTime;

    /** 小计 = price × num（仅有效商品计算） */
    public BigDecimal getSubtotal() {
        if (price == null || num == null || productStatus == null || productStatus != 1) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(num));
    }
}