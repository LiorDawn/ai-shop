package org.example.aishop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "商品评价信息")
public class CommentDTO {
    @Schema(description = "评价ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "图片（逗号分隔）")
    private String images;

    @Schema(description = "图片列表")
    private List<String> imageList;

    @Schema(description = "评分：1-5")
    private Integer score;

    @Schema(description = "状态：0=隐藏，1=显示")
    private Integer status;

    @Schema(description = "商家回复")
    private String reply;

    @Schema(description = "回复时间")
    private Date replyTime;

    @Schema(description = "创建时间")
    private Date createTime;
}