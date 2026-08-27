package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "购物车结算信息")
public class CartSettleVO {
    @Schema(description = "各店铺分组")
    private List<ShopCartGroup> shopGroups;

    @Schema(description = "选中总件数")
    private Integer totalNum;

    @Schema(description = "合计金额")
    private BigDecimal totalPrice;

    @Schema(description = "是否跨店铺（不同店铺需拆单）")
    private boolean crossShop;

    @Data
    @Schema(description = "店铺购物车分组")
    public static class ShopCartGroup {
        @Schema(description = "店铺ID")
        private Long shopId;

        @Schema(description = "店铺名称")
        private String shopName;

        @Schema(description = "商品列表")
        private List<CartItemVO> items;
    }
}