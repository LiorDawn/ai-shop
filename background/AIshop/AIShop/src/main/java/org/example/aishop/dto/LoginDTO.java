package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录请求参数")
public class LoginDTO {
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "验证码")
    private String code;
}