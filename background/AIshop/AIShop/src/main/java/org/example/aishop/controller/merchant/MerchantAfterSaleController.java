package org.example.aishop.controller.merchant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.AfterSaleDTO;
import org.example.aishop.dto.AfterSaleDetailDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.service.order.AfterSaleService;
import org.example.aishop.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "商家售后管理", description = "商家审核/处理售后申请")
@RestController
@RequestMapping("/merchant/after-sale")
public class MerchantAfterSaleController {

    @Autowired
    private AfterSaleService afterSaleService;

    @Autowired
    private ProductService productService;

    @Operation(summary = "商家售后列表", description = "分页查询当前店铺售后申请")
    @GetMapping("/list")
    public Result<Page<AfterSaleDTO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        Long shopId = productService.getCurrentMerchantShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        Page<AfterSaleDTO> page = afterSaleService.merchantPageList(shopId, current, size,
                orderNo, auditStatus, type, startTime, endTime);
        return Result.success("查询成功", page);
    }

    @Operation(summary = "商家查看售后详情")
    @GetMapping("/{id}")
    public Result<AfterSaleDetailDTO> detail(@PathVariable Long id) {
        Long shopId = productService.getCurrentMerchantShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        AfterSaleDetailDTO detail = afterSaleService.merchantGetDetail(shopId, id);
        return Result.success("查询成功", detail);
    }

    @Operation(summary = "商家处理售后", description = "同意/拒绝售后申请")
    @PostMapping("/audit")
    public Result<Void> audit(@RequestParam Long id,
                              @RequestParam Integer auditStatus,
                              @RequestParam String auditRemark,
                              @RequestParam(required = false) String returnAddress) {
        Long shopId = productService.getCurrentMerchantShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        org.example.aishop.dto.AuditRequestDTO dto = new org.example.aishop.dto.AuditRequestDTO();
        dto.setId(id);
        dto.setAuditStatus(auditStatus);
        dto.setAuditRemark(auditRemark);
        dto.setReturnAddress(returnAddress);
        afterSaleService.merchantAudit(dto, "商家");
        return Result.success("处理成功");
    }

    /** 商家确认收货完成退款 */
    @PostMapping("/finish/{id}")
    public Result<Void> finish(@PathVariable Long id) {
        Long shopId = productService.getCurrentMerchantShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        afterSaleService.finish(id);
        return Result.success("退款完成");
    }
}