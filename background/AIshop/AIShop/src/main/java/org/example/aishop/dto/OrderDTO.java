package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(description = "订单信息")
public class OrderDTO {
    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "总金额")
    private BigDecimal totalPrice;

    @Schema(description = "优惠券减免")
    private BigDecimal couponPrice;

    @Schema(description = "实付金额")
    private BigDecimal actualPrice;

    @Schema(description = "支付方式：1=微信，2=支付宝")
    private Integer payType;

    @Schema(description = "支付状态：0=未支付，1=已支付")
    private Integer payStatus;

    @Schema(description = "订单状态：0=待付款，1=待发货，2=待收货，3=已完成，-1=已取消")
    private Integer orderStatus;

    @Schema(description = "地址ID")
    private Long addressId;

    @Schema(description = "物流单号")
    private String logistics;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "支付时间")
    private Date payTime;

    @Schema(description = "发货时间")
    private Date deliverTime;

    @Schema(description = "完成时间")
    private Date finishTime;
}