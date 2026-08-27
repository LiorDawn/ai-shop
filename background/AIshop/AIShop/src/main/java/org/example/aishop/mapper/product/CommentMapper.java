package org.example.aishop.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.example.aishop.dto.CommentDTO;
import org.example.aishop.entity.product.Comment;

import java.util.Date;
import java.util.Set;

public interface CommentMapper extends BaseMapper<Comment> {

    /** 分页查询评论列表（关联商品、用户、店铺） */
    Page<CommentDTO> selectCommentPage(Page<CommentDTO> page,
                                       @Param("productIds") Set<Long> productIds,
                                       @Param("score") Integer score,
                                       @Param("status") Integer status,
                                       @Param("startTime") Date startTime,
                                       @Param("endTime") Date endTime,
                                       @Param("shopId") Long shopId,
                                       @Param("userId") Long userId,
                                       @Param("hasReply") Integer hasReply);

    /** 单条评论详情（关联商品、用户、店铺） */
    CommentDTO selectCommentDetail(@Param("id") Long id);
}