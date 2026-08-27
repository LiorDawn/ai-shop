package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "店铺信息")
public class ShopDTO {
    @Schema(description = "店铺ID")
    private Long id;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "店铺Logo")
    private String shopLogo;

    @Schema(description = "店铺简介")
    private String intro;

    @Schema(description = "状态：0=关闭，1=营业")
    private Integer status;

    @Schema(description = "创建时间")
    private String createTime;
}