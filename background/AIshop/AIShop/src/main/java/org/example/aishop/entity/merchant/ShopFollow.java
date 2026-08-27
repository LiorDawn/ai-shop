package org.example.aishop.entity.merchant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("shop_follow")
public class ShopFollow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long shopId;
    private Date createTime;
}