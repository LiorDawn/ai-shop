package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "验证码请求参数")
public class SmsCodeDTO {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;
}