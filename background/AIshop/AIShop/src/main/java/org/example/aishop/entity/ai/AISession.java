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
 * AI会话
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_session")
public class AISession implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 会话标题 */
    private String title;
    /** 创建时间 */
    private Date createTime;
    /** 最后操作时间 */
    private Date lastTime;
}