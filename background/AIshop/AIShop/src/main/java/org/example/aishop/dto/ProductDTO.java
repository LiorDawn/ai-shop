package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "商品信息")
public class ProductDTO {
    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "主图URL")
    private String image;

    @Schema(description = "状态：1=上架，0=下架")
    private Integer status;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "热度值（浏览量）")
    private Integer viewPoint;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "商品图片列表")
    private List<ProductImageDTO> imageList;

    @Schema(description = "SKU规格列表")
    private List<ProductSkuDTO> skuList;
}
