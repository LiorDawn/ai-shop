package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.dto.CommentDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.product.CommentService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品评论", description = "商品评价查询、用户发表/查看评价")
@RestController
@RequestMapping("/comment")
public class UserCommentController {

    @Autowired
    private CommentService commentService;

    @Operation(summary = "商品评价列表", description = "按商品分页查询用户评价")
    @GetMapping("/product/{productId}")
    public Result<Page<CommentDTO>> productComments(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @PathVariable Long productId) {
        Page<CommentDTO> page = commentService.pageByProduct(current, size, productId);
        return Result.success("查询成功", page);
    }

    @RepeatSubmit(prefix = "repeat:submit:comment:", leaseTime = 3, message = "评价正在提交中，请勿重复提交")
    @PostMapping("/add")
    public Result<Void> add(@RequestParam Long orderId,
                            @RequestParam Long productId,
                            @RequestParam Long shopId,
                            @RequestParam Integer score,
                            @RequestParam(required = false) String content,
                            @RequestParam(required = false) String images) {
        Long userId = UserHolder.getUserId();
        commentService.addComment(orderId, productId, shopId, userId, score, content, images);
        return Result.success("评价成功");
    }

    @Operation(summary = "我的评价列表")
    @GetMapping("/self")
    public Result<Page<CommentDTO>> self(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserHolder.getUserId();
        Page<CommentDTO> page = commentService.pageMyComments(current, size, userId);
        return Result.success("查询成功", page);
    }
}