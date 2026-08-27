package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "密码登录请求参数")
public class LoginByPwdDTO {
    @Schema(description = "手机号或邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String account;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "类型：1=手机，2=邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;
}