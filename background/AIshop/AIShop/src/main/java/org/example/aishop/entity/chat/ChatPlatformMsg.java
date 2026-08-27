package org.example.aishop.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("chat_platform_msg")
public class ChatPlatformMsg {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer sendType;
    private Long sendId;
    private Long receiveId;
    private Integer msgType;
    private String content;
    private String extraData;
    private Integer isRead;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Integer getSendType() { return sendType; }
    public void setSendType(Integer sendType) { this.sendType = sendType; }
    public Long getSendId() { return sendId; }
    public void setSendId(Long sendId) { this.sendId = sendId; }
    public Long getReceiveId() { return receiveId; }
    public void setReceiveId(Long receiveId) { this.receiveId = receiveId; }
    public Integer getMsgType() { return msgType; }
    public void setMsgType(Integer msgType) { this.msgType = msgType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}