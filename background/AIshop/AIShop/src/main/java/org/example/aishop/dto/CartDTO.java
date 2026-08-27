package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "购物车添加/更新参数")
public class CartDTO {
    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Schema(description = "SKU ID（可选）")
    private Long skuId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer num;
}