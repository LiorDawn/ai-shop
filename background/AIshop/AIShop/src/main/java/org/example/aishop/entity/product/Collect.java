package org.example.aishop.entity.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("collect")
public class Collect {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Date createTime;
}
