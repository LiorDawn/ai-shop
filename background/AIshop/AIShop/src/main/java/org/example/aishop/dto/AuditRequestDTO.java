package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "售后审核请求参数")
public class AuditRequestDTO {
    @Schema(description = "售后记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "审核状态：1=通过，2=驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer auditStatus;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "退货地址（退货退款时填写）")
    private String returnAddress;
}