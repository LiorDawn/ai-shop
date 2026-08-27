package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "商家信息")
public class MerchantDTO {
    @Schema(description = "商家ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "营业执照号")
    private String licenseNo;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "状态：0=待审核，1=正常，2=禁用")
    private Integer status;

    @Schema(description = "审核状态：0=待审核，1=已通过，2=已驳回")
    private Integer auditStatus;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "审核时间")
    private String auditTime;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "关联店铺信息")
    private ShopDTO shop;
}