package org.example.aishop.controller.merchant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.CommentDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.service.product.CommentService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "商家评价管理", description = "商家查看/回复商品评价")
@RestController
@RequestMapping("/merchant/comment")
public class MerchantCommentController {

    @Autowired
    private CommentService commentService;

    /** 分页查询评价 */
    @GetMapping("/page")
    public Result<Page<CommentDTO>> page(@RequestParam(defaultValue = "1") Integer current,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) Integer score,
                                         @RequestParam(required = false) String productName,
                                         @RequestParam(required = false) Integer hasReply) {
        Long shopId = getCurrentShopId();
        Page<CommentDTO> page = commentService.pageByShop(current, size, shopId, score, productName, hasReply);
        return Result.success("查询成功", page);
    }

    @Operation(summary = "评价详情")
    @GetMapping("/{id}")
    public Result<CommentDTO> detail(@PathVariable Long id) {
        CommentDTO dto = commentService.getDetail(id);
        // 校验该评价是否属于当前商家的店铺
        Long shopId = getCurrentShopId();
        if (!shopId.equals(dto.getShopId())) {
            throw new BusinessException(403, "无权查看该评价");
        }
        return Result.success("查询成功", dto);
    }

    @Operation(summary = "回复评价")
    @PutMapping("/reply/{id}")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reply = body.get("reply");
        // 校验权限
        Long shopId = getCurrentShopId();
        CommentDTO dto = commentService.getDetail(id);
        if (!shopId.equals(dto.getShopId())) {
            throw new BusinessException(403, "无权回复该评价");
        }
        commentService.reply(id, reply);
        return Result.success("回复成功");
    }

    /** 隐藏/显示评价 */
    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(400, "状态值不合法，1=正常 2=隐藏");
        }
        // 校验权限
        Long shopId = getCurrentShopId();
        CommentDTO dto = commentService.getDetail(id);
        if (!shopId.equals(dto.getShopId())) {
            throw new BusinessException(403, "无权操作该评价");
        }
        commentService.toggleStatus(id, status);
        return Result.success(status == 1 ? "评价已显示" : "评价已隐藏");
    }

    // ===== 辅助方法 =====

    private Long getCurrentShopId() {
        Long shopId = UserHolder.getShopId();
        if (shopId == null) {
            throw new BusinessException(401, "商家未登录或未绑定店铺");
        }
        return shopId;
    }
}