package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录成功返回")
public class LoginVO {

    @Schema(description = "JWT令牌")
    private String token;

    @Schema(description = "用户信息")
    private UserDTO user;
}