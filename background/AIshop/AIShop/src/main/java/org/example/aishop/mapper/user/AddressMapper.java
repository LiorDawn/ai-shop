package org.example.aishop.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aishop.entity.user.Address;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}