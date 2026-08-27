package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商品SKU规格")
public class ProductSkuDTO {
    @Schema(description = "SKU ID")
    private Long id;

    @Schema(description = "规格描述（如：红色/L）")
    private String spec;

    @Schema(description = "规格价格")
    private BigDecimal price;

    @Schema(description = "规格库存")
    private Integer stock;
}