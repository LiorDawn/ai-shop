package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class OrderDetailDTO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private BigDecimal totalPrice;
    private BigDecimal couponPrice;
    private BigDecimal actualPrice;
    private Integer payType;
    private Integer payStatus;
    private Integer orderStatus;
    private Long addressId;
    private String logistics;
    private String remark;
    private Date createTime;
    private Date payTime;
    private Date deliverTime;
    private Date finishTime;

    // 收货地址
    private String receiver;
    private String receiverPhone;
    private String address;

    // 商品明细
    private List<OrderItemDTO> items;

    @Data
    @Schema(description = "订单商品明细")
    public static class OrderItemDTO {
        @Schema(description = "明细ID")
        private Long id;

        @Schema(description = "订单ID")
        private Long orderId;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品图片")
        private String productImage;

        @Schema(description = "SKU ID")
        private Long skuId;

        @Schema(description = "规格描述")
        private String spec;

        @Schema(description = "店铺ID")
        private Long shopId;

        @Schema(description = "店铺名称")
        private String shopName;
        @Schema(description = "数量")
        private Integer num;

        @Schema(description = "单价")
        private BigDecimal price;

        @Schema(description = "小计金额")
        private BigDecimal subtotal;

        @Schema(description = "售后单ID（null表示未申请售后）")
        private Long afterSaleId;

        @Schema(description = "是否已评价")
        private Boolean hasComment;
    }
}