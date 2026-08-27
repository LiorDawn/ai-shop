package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.AfterSaleDTO;
import org.example.aishop.dto.AfterSaleDetailDTO;
import org.example.aishop.dto.AuditRequestDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.order.AfterSaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "管理端售后管理", description = "全平台售后审核、处理")
@RestController
@RequestMapping("/admin/after-sale")
public class AfterSaleController {

    @Autowired
    private AfterSaleService afterSaleService;

    @Operation(summary = "售后分页查询", description = "按订单号、审核状态、时间筛选")
    @GetMapping("/list")
    public Result<Page<AfterSaleDTO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        Page<AfterSaleDTO> page = afterSaleService.pageList(current, size, orderNo, auditStatus, startTime, endTime);
        return Result.success("查询成功", page);
    }

    @GetMapping("/{id}")
    public Result<AfterSaleDetailDTO> detail(@PathVariable Long id) {
        AfterSaleDetailDTO detail = afterSaleService.getDetail(id);
        return Result.success("查询成功", detail);
    }

    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody AuditRequestDTO dto,
                              @RequestParam(defaultValue = "管理员") String auditBy) {
        afterSaleService.audit(dto, auditBy);
        return Result.success("审核成功");
    }

    @Operation(summary = "完成售后")
    @PostMapping("/finish/{id}")
    public Result<Void> finish(@PathVariable Long id) {
        afterSaleService.finish(id);
        return Result.success("操作成功");
    }
}