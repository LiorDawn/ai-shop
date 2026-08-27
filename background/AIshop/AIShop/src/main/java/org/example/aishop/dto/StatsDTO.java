package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "数据统计信息")
public class StatsDTO {
    @Schema(description = "总销售额（已完成订单）")
    private BigDecimal totalSales;

    @Schema(description = "总订单量")
    private int totalOrders;

    @Schema(description = "总用户数")
    private int totalUsers;

    @Schema(description = "优惠券使用率（百分比字符串）")
    private String couponUsageRate;

    @Schema(description = "售后率（百分比字符串）")
    private String afterSaleRate;

    @Schema(description = "商品销量排行")
    private List<ProductSalesRankVO> productSalesRank;

    @Data
    @Schema(description = "商品销量排行项")
    public static class ProductSalesRankVO {
        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品图片")
        private String productImage;

        @Schema(description = "总销量")
        private int totalSales;
    }
}