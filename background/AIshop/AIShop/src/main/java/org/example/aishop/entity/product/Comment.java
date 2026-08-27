package org.example.aishop.entity.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private Long shopId;
    private Long userId;
    private String content;
    private String images;
    private Integer score;
    private Integer status;     // 1正常 2隐藏
    private String reply;
    private Date replyTime;
    private Integer delFlag;    // 0未删 1已删
    private Date createTime;
}
