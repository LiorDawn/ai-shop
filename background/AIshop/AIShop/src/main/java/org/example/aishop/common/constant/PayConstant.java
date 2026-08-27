package org.example.aishop.common.constant;

/**
 * 支付相关常量
 */
public class PayConstant {

    /** 支付方式：支付宝 */
    public static final int PAY_TYPE_ALIPAY = 1;

    /** 支付状态：未支付 */
    public static final int PAY_STATUS_UNPAID = 0;
    /** 支付状态：已支付 */
    public static final int PAY_STATUS_PAID = 1;

    /** 订单状态：待付款 */
    public static final int ORDER_STATUS_PENDING_PAY = 0;
    /** 订单状态：待发货 */
    public static final int ORDER_STATUS_PENDING_DELIVERY = 1;
    /** 订单状态：待收货 */
    public static final int ORDER_STATUS_PENDING_RECEIVE = 2;
    /** 订单状态：已完成 */
    public static final int ORDER_STATUS_COMPLETED = 3;
    /** 订单状态：已取消 */
    public static final int ORDER_STATUS_CANCELLED = 4;
}