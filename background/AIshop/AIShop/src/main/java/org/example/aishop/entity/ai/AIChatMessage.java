package org.example.aishop.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * AI聊天消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_message")
public class AIChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属会话ID */
    private Long sessionId;
    /** 消息文本 */
    private String content;
    /** 上传图片地址（多模态） */
    private String imgUrl;
    /** 角色：user / assistant */
    private String role;
    /** 创建时间 */
    private Date createTime;
}