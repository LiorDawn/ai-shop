package org.example.aishop.service.coupon;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.aishop.common.result.Result;
import org.example.aishop.entity.coupon.FlashSale;

public interface FlashSaleService extends IService<FlashSale> {

    /**
     * 获取秒杀活动列表（只返回进行中和即将开始的）
     */
    Result<?> listActive();

    /**
     * 获取秒杀商品详情 + 动态签名
     */
    Result<?> detail(Long flashSaleId);

    /**
     * 获取秒杀地址（带签名验证）
     */
    Result<?> getSeckillUrl(Long flashSaleId, String verifyCode, String sign);

    /**
     * 执行秒杀下单
     */
    Result<?> executeSeckill(Long flashSaleId, String sign, Long userId);

    /**
     * 轮询秒杀结果
     */
    Result<?> pollResult(String requestId);
}