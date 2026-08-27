package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户资料修改请求参数")
public class UserProfileUpdateDTO {
    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "性别：0=未知，1=男，2=女")
    private Integer gender;

    @Schema(description = "个性签名")
    private String signature;

    @Schema(description = "头像URL")
    private String avatar;
}