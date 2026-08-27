package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.CommentDTO;
import org.example.aishop.common.result.Result;
import org.example.aishop.service.product.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "管理端评价管理", description = "全平台评价查询、回复、状态管理")
@RestController
@RequestMapping("/admin/comment")
public class AdminCommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/list")
    public Result<Page<CommentDTO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer score,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        Page<CommentDTO> page = commentService.pageList(current, size, productName, score, status, startTime, endTime);
        return Result.success("查询成功", page);
    }

    @GetMapping("/{id}")
    public Result<CommentDTO> detail(@PathVariable Long id) {
        CommentDTO detail = commentService.getDetail(id);
        return Result.success("查询成功", detail);
    }

    @PostMapping("/reply")
    public Result<Void> reply(@RequestParam Long id, @RequestParam String reply) {
        commentService.reply(id, reply);
        return Result.success("回复成功");
    }

    @Operation(summary = "更新评价状态")
    @PutMapping("/status/{id}")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        commentService.toggleStatus(id, status);
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success("删除成功");
    }
}