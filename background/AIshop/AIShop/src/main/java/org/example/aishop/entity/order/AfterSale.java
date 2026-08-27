package org.example.aishop.entity.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("after_sale")
public class AfterSale {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Long shopId;
    private Long productId;
    private Integer type;        // 0仅退款 1退货退款 2换货
    private BigDecimal amount;   // 申请退款金额
    private String reason;       // 售后原因
    private String description;  // 问题描述
    private String images;       // 凭证图片
    private Integer auditStatus; // 0待审核 1已通过 2已驳回 3待退货 4已完成
    private String auditBy;      // 审核人
    private Date auditTime;      // 审核时间
    private String auditRemark;  // 审核备注
    private String returnAddress; // 商家退货地址
    private String expressCompany; // 快递公司
    private String expressNo;     // 物流单号
    private Date finishTime;     // 完成时间
    private Integer delFlag;     // 0未删 1已删
    private Date createTime;
}
