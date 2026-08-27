package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.MerchantDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.merchant.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端商家审核", description = "商家入驻审核、管理")
@RestController
@RequestMapping("/admin/merchant")
public class AdminMerchantController {

    @Autowired
    private MerchantService merchantService;

    @Operation(summary = "商家分页查询")
    @GetMapping("/page")
    public Result<Page<MerchantDTO>> page(@RequestParam(defaultValue = "1") Integer current,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String merchantName,
                                          @RequestParam(required = false) Integer status) {
        Page<MerchantDTO> page = (Page<MerchantDTO>) merchantService.pageMerchants(current, size, merchantName, status);
        return Result.success("查询成功", page);
    }

    @GetMapping("/{id}")
    public Result<MerchantDTO> getById(@PathVariable Long id) {
        MerchantDTO dto = merchantService.getMerchantById(id);
        return Result.success("查询成功", dto);
    }

    @Operation(summary = "审核商家", description = "审核通过/驳回商家入驻申请")
    @PutMapping("/audit/{id}")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer auditStatus = (Integer) body.get("status");
        String auditRemark = (String) body.get("auditRemark");
        merchantService.auditMerchant(id, auditStatus, auditRemark);
        String msg = auditStatus == 1 ? "审核通过" : "审核驳回";
        return Result.success(msg);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        merchantService.deleteMerchant(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        merchantService.deleteBatchMerchants(ids);
        return Result.success("批量删除成功");
    }
}