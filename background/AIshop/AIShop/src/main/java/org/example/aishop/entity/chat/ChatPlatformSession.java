package org.example.aishop.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("chat_platform_session")
public class ChatPlatformSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long adminId;
    private Integer status;
    private Date createTime;
    private Date endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}