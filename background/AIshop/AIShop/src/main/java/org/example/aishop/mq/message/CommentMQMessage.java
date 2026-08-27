package org.example.aishop.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 评论异步处理 MQ 消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentMQMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long commentId;
    private Long productId;
    private Long shopId;
    private Long userId;
    private Integer score;
    private String type; // "CREATE" 新增评论
}