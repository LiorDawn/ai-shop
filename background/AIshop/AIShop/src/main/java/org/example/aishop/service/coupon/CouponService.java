package org.example.aishop.service.coupon;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.CouponDTO;
import org.example.aishop.dto.CouponRecordVO;
import org.example.aishop.entity.coupon.Coupon;

import java.util.List;

public interface CouponService {
    Page<CouponDTO> pageCoupons(Integer current, Integer size, String name, Integer status);

    CouponDTO getCouponById(Long id);

    void addCoupon(Coupon coupon);

    void updateCoupon(Coupon coupon);

    void deleteCoupon(Long id);

    void toggleStatus(Long id);

    Page<CouponRecordVO> pageRecords(Integer current, Integer size, Long couponId);

    /** 用户领取优惠券（Lua 原子扣减） */
    void claimCoupon(Long userId, Long couponId);

    /** 用户端：获取可领取的优惠券列表 */
    Page<CouponDTO> pageAvailable(Integer current, Integer size, Long userId);

    /** 用户端：获取我的优惠券 */
    Page<CouponRecordVO> pageMyCoupons(Long userId, Integer current, Integer size, Integer status);

    /** 用户端：获取我的可用优惠券（未使用且未过期，用于下单） */
    List<CouponDTO> listMyAvailable(Long userId);
}