package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "普通用户注册参数")
public class RegisterDTO {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "用户名（可选，默认生成）")
    private String username;
}