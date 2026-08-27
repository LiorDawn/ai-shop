package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(description = "售后申请信息")
public class AfterSaleDTO {
    @Schema(description = "售后ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单项ID")
    private Long orderItemId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "商品ID")
    private Long productId;

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

    @Schema(description = "凭证图片")
    private String images;

    @Schema(description = "审核状态：0=待审核，1=已通过，2=已驳回")
    private Integer auditStatus;

    @Schema(description = "审核人")
    private String auditBy;

    @Schema(description = "审核时间")
    private Date auditTime;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "完成时间")
    private Date finishTime;

    @Schema(description = "状态文字说明")
    private String statusText;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "退货地址")
    private String returnAddress;

    @Schema(description = "物流公司")
    private String expressCompany;

    @Schema(description = "物流单号")
    private String expressNo;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "规格描述")
    private String spec;

    @Schema(description = "数量")
    private Integer num;
}