package org.example.aishop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String operator;
    private String operType;
    private String content;
    private String ip;
    private Integer status;
    private Date createTime;
}
