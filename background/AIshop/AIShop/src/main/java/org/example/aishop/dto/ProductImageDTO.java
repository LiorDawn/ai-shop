package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "商品图片")
public class ProductImageDTO {
    @Schema(description = "图片ID")
    private Long id;

    @Schema(description = "图片URL")
    private String imageUrl;
}