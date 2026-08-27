package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.dto.CouponDTO;
import org.example.aishop.dto.CouponRecordVO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.coupon.CouponService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户优惠券", description = "优惠券领取、我的优惠券、可用优惠券查询")
@RestController
@RequestMapping("/coupon")
public class UserCouponController {

    @Autowired
    private CouponService couponService;

    @Operation(summary = "可领取优惠券列表")
    @GetMapping("/available")
    public Result<Page<CouponDTO>> available(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserHolder.getUserId();
        Page<CouponDTO> page = couponService.pageAvailable(current, size, userId);
        return Result.success("查询成功", page);
    }

    /** 领取优惠券 */
    @RepeatSubmit(prefix = "repeat:submit:coupon:", leaseTime = 3, message = "正在领取中，请勿重复点击")
    @PostMapping("/receive/{couponId}")
    public Result<Void> receive(@PathVariable Long couponId) {
        Long userId = UserHolder.getUserId();
        couponService.claimCoupon(userId, couponId);
        return Result.success("领取成功");
    }

    @Operation(summary = "我的优惠券", description = "分页查询我的优惠券，status 可筛选可用/已用/已过期")
    @GetMapping("/my")
    public Result<Page<CouponRecordVO>> my(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Long userId = UserHolder.getUserId();
        Page<CouponRecordVO> page = couponService.pageMyCoupons(userId, current, size, status);
        return Result.success("查询成功", page);
    }

    @Operation(summary = "我的可用优惠券", description = "用于下单页面获取可用优惠券")
    @GetMapping("/my-available")
    public Result<List<CouponDTO>> myAvailable() {
        Long userId = UserHolder.getUserId();
        List<CouponDTO> list = couponService.listMyAvailable(userId);
        return Result.success("查询成功", list);
    }
}