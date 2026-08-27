package org.example.aishop.service.pay.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.aishop.config.AlipayConfig;
import org.example.aishop.common.constant.PayConstant;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.entity.order.OrderItem;
import org.example.aishop.entity.order.Orders;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.order.OrderItemMapper;
import org.example.aishop.mapper.order.OrderMapper;
import org.example.aishop.service.pay.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PayServiceImpl implements PayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String createPayForm(Long orderId, Long userId) {
        // 1. 查询订单
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此订单");
        }
        if (order.getPayStatus() != PayConstant.PAY_STATUS_UNPAID) {
            throw new BusinessException(400, "订单已支付，请勿重复支付");
        }

        // 2. 构建支付宝支付请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 同步跳转地址（支付完成后跳回支付页，自动识别支付成功状态展示成功界面）
        // 顺序：#/pay/{orderId} 保持 hash 路由格式
        request.setReturnUrl(alipayConfig.getReturnUrl() + "/" + orderId);
        // 异步通知地址（支付宝主动 POST 通知结果）
        request.setNotifyUrl(alipayConfig.getNotifyUrl());

        // 业务参数
        request.setBizContent("{" +
                "    \"out_trade_no\":\"" + order.getOrderNo() + "\"," +
                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
                "    \"total_amount\":\"" + order.getActualPrice().setScale(2, RoundingMode.HALF_UP) + "\"," +
                "    \"subject\":\"AI智能商城-订单支付\"" +
                "  }");

        try {
            // 3. 调用 SDK 生成支付表单
            String form = alipayClient.pageExecute(request).getBody();
            log.info("支付宝支付表单生成成功, orderNo=" + order.getOrderNo());
            return form;
        } catch (AlipayApiException e) {
            log.error("支付宝支付表单生成失败, orderNo=" + order.getOrderNo(), e);
            throw new BusinessException(500, "支付请求失败：" + e.getErrMsg());
        }
    }

    @Override
    public String handleNotify(Map<String, String> params) {
        log.info("收到支付宝异步通知, params=" + params);

        try {
            // 1. 验签
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
            if (!signVerified) {
                log.error("支付宝通知验签失败");
                return "fail";
            }

            // 2. 获取关键参数
            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String tradeNo = params.get("trade_no"); // 支付宝交易号
            String totalAmount = params.get("total_amount");

            log.info("支付宝通知验签成功, outTradeNo=" + outTradeNo + ", tradeStatus=" + tradeStatus);

            // 3. 只处理交易成功状态
            if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                log.info("非成功状态，忽略处理, tradeStatus=" + tradeStatus);
                return "success";
            }

            // 4. 查询订单并更新
            Orders order = orderMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Orders>()
                    .eq(Orders::getOrderNo, outTradeNo));
            LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Orders::getOrderNo, outTradeNo)
                    .eq(Orders::getPayStatus, PayConstant.PAY_STATUS_UNPAID) // 防止重复更新
                    .set(Orders::getPayStatus, PayConstant.PAY_STATUS_PAID)
                    .set(Orders::getOrderStatus, PayConstant.ORDER_STATUS_PENDING_DELIVERY);

            boolean updated = orderMapper.update(null, wrapper) > 0;
            if (updated) {
                log.info("订单支付成功, orderNo=" + outTradeNo + ", tradeNo=" + tradeNo + ", amount=" + totalAmount);
                if (order != null) {
                    invalidateMerchantStatsCache(order.getId());
                }
            } else {
                log.warn("订单更新失败（可能已支付或不存在）, orderNo=" + outTradeNo);
            }

            return "success";

        } catch (AlipayApiException e) {
            log.error("支付宝通知处理异常", e);
            return "fail";
        }
    }

    @Override
    public void simulatePay(Long orderId, Long userId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此订单");
        }
        if (order.getPayStatus() != PayConstant.PAY_STATUS_UNPAID) {
            throw new BusinessException(400, "订单已支付，请勿重复支付");
        }
        if (order.getOrderStatus() != PayConstant.ORDER_STATUS_PENDING_PAY) {
            throw new BusinessException(400, "订单状态异常，当前仅待付款订单可模拟支付");
        }

        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Orders::getId, orderId)
                .eq(Orders::getPayStatus, PayConstant.PAY_STATUS_UNPAID)
                .set(Orders::getPayStatus, PayConstant.PAY_STATUS_PAID)
                .set(Orders::getOrderStatus, PayConstant.ORDER_STATUS_PENDING_DELIVERY)
                .set(Orders::getPayType, PayConstant.PAY_TYPE_ALIPAY);

        boolean updated = orderMapper.update(null, wrapper) > 0;
        if (updated) {
            log.info("模拟支付成功, orderId=" + orderId + ", orderNo=" + order.getOrderNo());
            // 清除订单相关店铺的统计数据缓存
            invalidateMerchantStatsCache(orderId);
        } else {
            throw new BusinessException(500, "模拟支付失败，请稍后重试");
        }
    }

    /**
     * 清除订单关联店铺的统计数据缓存
     */
    private void invalidateMerchantStatsCache(Long orderId) {
        try {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, orderId));
            for (Long shopId : items.stream().map(OrderItem::getShopId).distinct().collect(Collectors.toList())) {
                stringRedisTemplate.delete(RedisConstant.statsMerchantOverviewKey(shopId));
                stringRedisTemplate.delete(RedisConstant.statsSalesRankingKey(shopId));
                stringRedisTemplate.delete(RedisConstant.statsOrderTrendKey(shopId));
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean queryPayStatus(Long orderId, Long userId) {
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作此订单");
        }
        // 已支付直接返回成功
        if (order.getPayStatus() == PayConstant.PAY_STATUS_PAID) {
            return true;
        }

        // 主动查询支付宝支付状态
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{\"out_trade_no\":\"" + order.getOrderNo() + "\"}");

        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            log.info("支付宝查询支付状态, orderNo=" + order.getOrderNo() + ", code=" + response.getCode() + ", subCode=" + response.getSubCode() + ", tradeStatus=" + response.getTradeStatus());

            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    // 支付宝确认已支付，更新订单
                    LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
                    wrapper.eq(Orders::getId, orderId)
                            .eq(Orders::getPayStatus, PayConstant.PAY_STATUS_UNPAID)
                            .set(Orders::getPayStatus, PayConstant.PAY_STATUS_PAID)
                            .set(Orders::getOrderStatus, PayConstant.ORDER_STATUS_PENDING_DELIVERY)
                            .set(Orders::getPayType, PayConstant.PAY_TYPE_ALIPAY);

                    boolean updated = orderMapper.update(null, wrapper) > 0;
                    if (updated) {
                        log.info("主动查询后确认支付成功, orderNo=" + order.getOrderNo() + ", tradeNo=" + response.getTradeNo());
                        invalidateMerchantStatsCache(orderId);
                    }
                    return true;
                }
            } else {
                // ACQ.TRADE_NOT_EXIST 等表示交易尚未创建或未支付，正常返回 false
                log.info("支付宝查询返回非成功状态, orderNo=" + order.getOrderNo() + ", code=" + response.getCode() + ", subCode=" + response.getSubCode() + ", msg=" + response.getMsg());
            }
        } catch (AlipayApiException e) {
            // 如果 SDK 以异常形式抛出（部分版本），检查是否是"交易不存在"
            if (e.getErrCode() != null && e.getErrCode().contains("TRADE_NOT_EXIST")) {
                log.info("交易在支付宝侧尚未创建, orderNo=" + order.getOrderNo() + ", errCode=" + e.getErrCode());
            } else {
                log.error("查询支付宝支付状态异常, orderNo=" + order.getOrderNo() + ", errCode=" + e.getErrCode() + ", errMsg=" + e.getErrMsg());
            }
        }

        return false;
    }
}