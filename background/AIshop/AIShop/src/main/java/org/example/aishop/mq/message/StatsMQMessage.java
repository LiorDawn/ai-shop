package org.example.aishop.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统计任务 MQ 消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsMQMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 统计类型：ADMIN_OVERVIEW / MERCHANT_OVERVIEW / SALES_RANKING / ORDER_TREND */
    private String statsType;

    /** 店铺ID（商家统计时使用，管理端统计为 null） */
    private Long shopId;

    public static StatsMQMessage adminOverview() {
        return new StatsMQMessage("ADMIN_OVERVIEW", null);
    }

    public static StatsMQMessage merchantOverview(Long shopId) {
        return new StatsMQMessage("MERCHANT_OVERVIEW", shopId);
    }

    public static StatsMQMessage salesRanking(Long shopId) {
        return new StatsMQMessage("SALES_RANKING", shopId);
    }

    public static StatsMQMessage orderTrend(Long shopId) {
        return new StatsMQMessage("ORDER_TREND", shopId);
    }
}