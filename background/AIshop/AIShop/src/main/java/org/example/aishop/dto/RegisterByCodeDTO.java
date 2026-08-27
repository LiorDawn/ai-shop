package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "验证码注册请求参数")
public class RegisterByCodeDTO {
    @Schema(description = "手机号或邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String account;

    @Schema(description = "6位验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "类型：1=手机，2=邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;
}