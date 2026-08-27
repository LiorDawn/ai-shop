package org.example.aishop.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("address")
public class Address {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String receiver;
    private String phone;
    private String address;
    private Integer isDefault;
}
