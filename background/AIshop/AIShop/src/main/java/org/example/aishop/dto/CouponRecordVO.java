package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Schema(description = "优惠券领取记录")
public class CouponRecordVO {
    @Schema(description = "领取记录ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "类型：1=满减，2=折扣")
    private Integer type;

    @Schema(description = "满多少可用")
    private BigDecimal minPrice;

    @Schema(description = "减免金额/折扣")
    private BigDecimal discount;

    @Schema(description = "生效时间")
    private Date startTime;

    @Schema(description = "过期时间")
    private Date endTime;

    @Schema(description = "状态：0=未使用，1=已使用，2=已过期")
    private Integer status;

    @Schema(description = "状态文字说明")
    private String statusText;

    @Schema(description = "领取时间")
    private Date createTime;
}