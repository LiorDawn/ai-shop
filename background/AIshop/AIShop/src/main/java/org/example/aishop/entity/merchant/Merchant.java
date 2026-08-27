package org.example.aishop.entity.merchant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String merchantName;
    private String licenseNo;
    private String contact;
    private String phone;
    private Integer status;
    private Integer auditStatus;  // 0待审核 1已通过 2已驳回
    private String auditRemark;
    private Date auditTime;
    private Date createTime;
}