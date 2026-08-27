package org.example.aishop.entity.merchant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("shop")
public class Shop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String shopName;
    private String shopLogo;
    private String intro;
    private Integer status;
    private Date createTime;
}