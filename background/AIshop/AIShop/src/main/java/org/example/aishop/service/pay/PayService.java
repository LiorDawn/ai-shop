package org.example.aishop.service.pay;

import java.util.Map;

/**
 * 支付服务
 */
public interface PayService {

    /**
     * 生成支付宝支付表单（页面自动跳转）
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     * @return 自动提交的 HTML 表单字符串
     */
    String createPayForm(Long orderId, Long userId);

    /**
     * 处理支付宝异步通知
     *
     * @param params 支付宝 POST 参数（不含 sign/sign_type）
     * @return "success" 或 "fail"
     */
    String handleNotify(Map<String, String> params);

    /**
     * 模拟支付成功（开发环境用，跳过支付宝异步通知）
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     */
    void simulatePay(Long orderId, Long userId);

    /**
     * 主动向支付宝查询订单支付状态（用于同步回调后确认支付结果）
     * 若支付宝确认已支付，则更新订单状态
     *
     * @param orderId 订单ID
     * @param userId  用户ID
     * @return true = 支付成功，false = 尚未支付
     */
    boolean queryPayStatus(Long orderId, Long userId);
}