package org.example.aishop.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aishop.entity.user.Role;
import org.example.aishop.mapper.user.RoleMapper;
import org.example.aishop.service.user.RoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}
