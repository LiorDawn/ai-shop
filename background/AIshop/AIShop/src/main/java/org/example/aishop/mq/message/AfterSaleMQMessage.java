package org.example.aishop.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 售后 MQ 消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AfterSaleMQMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息类型：NOTIFY / DATA_UPDATE / DELAY_CLOSE */
    private String msgType;

    /** 售后单ID */
    private Long afterSaleId;
    /** 订单ID */
    private Long orderId;
    /** 用户ID */
    private Long userId;
    /** 店铺ID */
    private Long shopId;
    /** 售后类型 */
    private Integer afterSaleType;
    /** 通知内容 */
    private String notifyContent;

    /** 通知消息 */
    public static AfterSaleMQMessage notify(Long afterSaleId, Long userId, Long shopId,
                                             String content) {
        AfterSaleMQMessage msg = new AfterSaleMQMessage();
        msg.msgType = "NOTIFY";
        msg.afterSaleId = afterSaleId;
        msg.userId = userId;
        msg.shopId = shopId;
        msg.notifyContent = content;
        return msg;
    }

    /** 数据更新消息 */
    public static AfterSaleMQMessage dataUpdate(Long afterSaleId, Long orderId, Long shopId) {
        AfterSaleMQMessage msg = new AfterSaleMQMessage();
        msg.msgType = "DATA_UPDATE";
        msg.afterSaleId = afterSaleId;
        msg.orderId = orderId;
        msg.shopId = shopId;
        return msg;
    }

    /** 延时关闭消息 */
    public static AfterSaleMQMessage delayClose(Long afterSaleId, Long orderId, Long shopId, Integer type) {
        AfterSaleMQMessage msg = new AfterSaleMQMessage();
        msg.msgType = "DELAY_CLOSE";
        msg.afterSaleId = afterSaleId;
        msg.orderId = orderId;
        msg.shopId = shopId;
        msg.afterSaleType = type;
        return msg;
    }
}