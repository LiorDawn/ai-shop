package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改密码请求参数")
public class PasswordChangeDTO {
    @Schema(description = "原密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    @Schema(description = "确认新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;
}