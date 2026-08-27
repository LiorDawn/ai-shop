package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.MerchantDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.service.merchant.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商家入驻", description = "商家入驻申请、信息修改（通用层）")
@RestController
@RequestMapping("/merchant")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    @PostMapping
    public Result<Void> add(@RequestBody Merchant merchant) {
        merchantService.addMerchant(merchant);
        return Result.success("申请提交成功，等待审核");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Merchant merchant) {
        merchantService.updateMerchant(merchant);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        merchantService.deleteMerchant(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "批量删除商家")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        merchantService.deleteBatchMerchants(ids);
        return Result.success("批量删除成功");
    }

    @PutMapping("/audit/{id}")
    public Result<Void> audit(@PathVariable Long id, 
                              @RequestParam Integer status,
                              @RequestParam String auditRemark) {
        merchantService.auditMerchant(id, status, auditRemark);
        String msg = status == 1 ? "审核通过" : "审核驳回";
        return Result.success(msg);
    }

    @Operation(summary = "根据ID查询商家")
    @GetMapping("/{id}")
    public Result<MerchantDTO> getById(@PathVariable Long id) {
        MerchantDTO dto = merchantService.getMerchantById(id);
        return Result.success("查询成功", dto);
    }

    @GetMapping("/list")
    public Result<List<MerchantDTO>> list() {
        List<MerchantDTO> list = merchantService.listMerchants();
        return Result.success("查询成功", list);
    }

    @Operation(summary = "商家分页查询")
    @GetMapping("/page")
    public Result<Page<MerchantDTO>> page(@RequestParam(defaultValue = "1") Integer current,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String merchantName,
                                          @RequestParam(required = false) Integer status) {
        Page<MerchantDTO> page = (Page<MerchantDTO>) merchantService.pageMerchants(current, size, merchantName, status);
        return Result.success("查询成功", page);
    }
}