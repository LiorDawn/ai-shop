package org.example.aishop.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aishop.entity.order.Cart;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}