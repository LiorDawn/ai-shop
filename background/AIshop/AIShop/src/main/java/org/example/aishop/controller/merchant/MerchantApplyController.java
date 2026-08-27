package org.example.aishop.controller.merchant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.dto.MerchantDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.entity.merchant.Merchant;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.service.merchant.MerchantService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "商家入驻申请", description = "用户提交入驻申请")
@RestController
@RequestMapping("/merchant/apply")
public class MerchantApplyController {

    @Autowired
    private MerchantService merchantService;

    @Operation(summary = "提交入驻申请", description = "用户提交商家入驻审核申请")
    @RepeatSubmit(prefix = "repeat:submit:merchant:apply:", leaseTime = 5, message = "入驻申请正在处理中，请勿重复提交")
    @PostMapping
    public Result<Void> submit(@RequestBody Map<String, String> body) {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }

        Merchant merchant = new Merchant();
        merchant.setUserId(currentUser.getId());
        merchant.setMerchantName(body.get("merchantName"));
        merchant.setLicenseNo(body.get("licenseNo"));
        merchant.setContact(body.get("contact"));
        merchant.setPhone(body.get("phone"));

        merchantService.addMerchant(merchant);
        return Result.success("入驻申请已提交，请等待审核");
    }

    /**
     * 查询当前用户的入驻申请状态
     * 返回 null 表示未申请
     */
    @GetMapping("/status")
    public Result<MerchantDTO> status() {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        MerchantDTO dto = merchantService.getMyApplication(currentUser.getId());
        return Result.success("查询成功", dto);
    }
}