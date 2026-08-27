package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "售后详情")
public class AfterSaleDetailDTO {
    @Schema(description = "售后ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "订单创建时间")
    private Date orderCreateTime;

    @Schema(description = "订单总金额")
    private BigDecimal orderTotalPrice;

    @Schema(description = "售后类型：1=退货退款，2=仅退款")
    private Integer type;

    @Schema(description = "类型文字说明")
    private String typeText;

    @Schema(description = "退款金额")
    private BigDecimal amount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "凭证图片（逗号分隔）")
    private String images;

    @Schema(description = "审核状态：0=待审核，1=已通过，2=已驳回")
    private Integer auditStatus;

    @Schema(description = "状态文字说明")
    private String statusText;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "审核人")
    private String auditBy;

    @Schema(description = "审核时间")
    private Date auditTime;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "退货地址")
    private String returnAddress;

    @Schema(description = "物流公司")
    private String expressCompany;

    @Schema(description = "物流单号")
    private String expressNo;

    @Schema(description = "商品明细")
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private String spec;
        private BigDecimal price;
        private Integer num;
    }
}