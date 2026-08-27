package org.example.aishop.mapper.product;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aishop.entity.product.Category;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}