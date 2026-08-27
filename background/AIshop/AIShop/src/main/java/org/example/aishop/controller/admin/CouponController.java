package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.CouponDTO;
import org.example.aishop.dto.CouponRecordVO;
import org.example.aishop.common.result.Result;
import org.example.aishop.entity.coupon.Coupon;
import org.example.aishop.service.coupon.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端优惠券管理", description = "优惠券CRUD、发放记录")
@RestController
@RequestMapping("/admin/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @GetMapping("/page")
    public Result<Page<CouponDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<CouponDTO> page = couponService.pageCoupons(current, size, name, status);
        return Result.success("查询成功", page);
    }

    @Operation(summary = "根据ID查询优惠券")
    @GetMapping("/{id}")
    public Result<CouponDTO> get(@PathVariable Long id) {
        return Result.success("查询成功", couponService.getCouponById(id));
    }

    @Operation(summary = "新增优惠券")
    @PostMapping
    public Result<Void> add(@RequestBody Coupon coupon) {
        couponService.addCoupon(coupon);
        return Result.success("新增成功");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Coupon coupon) {
        couponService.updateCoupon(coupon);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return Result.success("删除成功");
    }

    @PutMapping("/toggle/{id}")
    public Result<Void> toggle(@PathVariable Long id) {
        couponService.toggleStatus(id);
        return Result.success("操作成功");
    }

    @Operation(summary = "优惠券领取记录")
    @GetMapping("/records")
    public Result<Page<CouponRecordVO>> records(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long couponId) {
        Page<CouponRecordVO> page = couponService.pageRecords(current, size, couponId);
        return Result.success("查询成功", page);
    }
}