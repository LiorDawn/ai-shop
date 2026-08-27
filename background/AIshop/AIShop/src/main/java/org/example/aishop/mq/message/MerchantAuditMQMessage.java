package org.example.aishop.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商家入驻审核后处理 MQ 消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuditMQMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 商家ID */
    private Long merchantId;
    /** 用户ID */
    private Long userId;
    /** 商家名称 */
    private String merchantName;
    /** 审核结果：1=通过 2=驳回 */
    private Integer auditStatus;
    /** 审核备注 */
    private String auditRemark;
}