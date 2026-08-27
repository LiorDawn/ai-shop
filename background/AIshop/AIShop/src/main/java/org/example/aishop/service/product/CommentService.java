package org.example.aishop.service.product;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.dto.CommentDTO;

import java.util.Date;

public interface CommentService {
    // 管理员：分页查询
    Page<CommentDTO> pageList(Integer current, Integer size,
                              String productName, Integer score, Integer status,
                              Date startTime, Date endTime);

    // 管理员：获取详情
    CommentDTO getDetail(Long id);

    // 管理员：回复
    void reply(Long id, String reply);

    // 管理员：切换状态（1正常/2隐藏）
    void toggleStatus(Long id, Integer status);

    // 管理员：删除（逻辑删除）
    void deleteComment(Long id);

    // ===== 用户端 API =====

    // 用户：查询商品评价（仅正常状态）
    Page<CommentDTO> pageByProduct(Integer current, Integer size, Long productId);

    // 用户：新增评价
    void addComment(Long orderId, Long productId, Long shopId, Long userId, Integer score, String content, String images);

    // 用户：查询我的评价
    Page<CommentDTO> pageMyComments(Integer current, Integer size, Long userId);

    // ===== 商家 API =====

    /** 商家：按店铺分页查询评价 */
    Page<CommentDTO> pageByShop(Integer current, Integer size, Long shopId,
                                Integer score, String productName,
                                Integer hasReply);
}