package org.example.aishop.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.dto.AfterSaleDTO;
import org.example.aishop.dto.AfterSaleDetailDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.order.AfterSaleService;
import io.swagger.v3.oas.annotations.Operation;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/after-sale")
public class UserAfterSaleController {

    @Autowired
    private AfterSaleService afterSaleService;

    /** 申请售后 */
    @RepeatSubmit(prefix = "repeat:submit:after-sale:", leaseTime = 3, message = "售后申请正在处理中，请勿重复提交")
    @PostMapping("/apply")
    public Result<Void> apply(@RequestParam Long orderId,
                              @RequestParam Long orderItemId,
                              @RequestParam Integer type,
                              @RequestParam BigDecimal amount,
                              @RequestParam String reason,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String images) {
        Long userId = UserHolder.getUserId();
        afterSaleService.apply(userId, orderId, orderItemId, type, amount, reason, description, images);
        return Result.success("申请已提交");
    }

    /** 我的售后列表 */
    @GetMapping("/list")
    public Result<Page<AfterSaleDTO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer auditStatus) {
        Long userId = UserHolder.getUserId();
        Page<AfterSaleDTO> page = afterSaleService.pageMyAfterSales(userId, current, size, auditStatus);
        return Result.success("查询成功", page);
    }

    @Operation(summary = "售后详情")
    @GetMapping("/{id}")
    public Result<AfterSaleDetailDTO> detail(@PathVariable Long id) {
        Long userId = UserHolder.getUserId();
        AfterSaleDetailDTO detail = afterSaleService.getMyDetail(userId, id);
        return Result.success("查询成功", detail);
    }

    /** 撤销售后申请 */
    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = UserHolder.getUserId();
        afterSaleService.cancel(userId, id);
        return Result.success("撤销成功");
    }

    /** 填写退货物流信息 */
    @PostMapping("/return-logistics/{id}")
    public Result<Void> returnLogistics(@PathVariable Long id,
                                        @RequestParam String expressCompany,
                                        @RequestParam String expressNo) {
        Long userId = UserHolder.getUserId();
        afterSaleService.returnLogistics(userId, id, expressCompany, expressNo);
        return Result.success("物流信息提交成功");
    }
}