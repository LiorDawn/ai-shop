package org.example.aishop.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aishop.entity.chat.ChatPlatformSession;

@Mapper
public interface ChatPlatformSessionMapper extends BaseMapper<ChatPlatformSession> {
}