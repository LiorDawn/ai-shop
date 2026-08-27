package org.example.aishop.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aishop.entity.ai.AIChatMessage;

@Mapper
public interface AIChatMapper extends BaseMapper<AIChatMessage> {
}