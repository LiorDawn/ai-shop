package org.example.aishop.service.user.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.entity.user.Role;
import org.example.aishop.entity.user.User;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.mapper.user.RoleMapper;
import org.example.aishop.mapper.user.UserMapper;
import org.example.aishop.service.user.UserService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public boolean save(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }
        if (getByUsername(user.getUsername()) != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (StringUtils.hasText(user.getPhone()) && getByPhone(user.getPhone()) != null) {
            throw new BusinessException(400, "手机号已被注册");
        }
        // BCrypt 哈希密码
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        return super.save(user);
    }

    @Override
    public boolean updateById(User user) {
        if (user.getId() == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        User exist = super.getById(user.getId());
        if (exist == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (StringUtils.hasText(user.getUsername()) && !user.getUsername().equals(exist.getUsername())) {
            if (getByUsername(user.getUsername()) != null) {
                throw new BusinessException(400, "用户名已存在");
            }
        }
        if (StringUtils.hasText(user.getPhone()) && !user.getPhone().equals(exist.getPhone())) {
            if (getByPhone(user.getPhone()) != null) {
                throw new BusinessException(400, "手机号已被注册");
            }
        }
        return super.updateById(user);
    }

    @Override
    public boolean removeById(Serializable id) {
        User exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return super.removeById(id);
    }

    @Override
    public boolean removeByIds(Collection<?> idList) {
        if (idList == null || idList.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的用户");
        }
        return super.removeByIds(idList);
    }

    @Override
    public void addUser(User user) {
        save(user);
    }

    @Override
    public void deleteUser(Long id) {
        // 不允许操作超级管理员
        checkNotSuperAdmin(id);
        removeById(id);
    }

    @Override
    public void deleteBatchUsers(List<Long> ids) {
        // 检查要删除的用户中是否有超级管理员
        for (Long id : ids) {
            checkNotSuperAdmin(id);
        }
        removeByIds(ids);
    }

    @Override
    public void updateUser(User user) {
        if (user.getId() != null) {
            checkNotSuperAdmin(user.getId());
        }
        updateById(user);
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        // 不允许操作超级管理员
        checkNotSuperAdmin(id);
        if (status != 0 && status != 1) {
            throw new BusinessException(400, "状态值不合法，只能为0或1");
        }
        User exist = super.getById(id);
        if (exist == null) {
            throw new BusinessException(404, "用户不存在");
        }
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        super.updateById(user);
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = super.getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toUserDTO(user);
    }

    @Override
    public List<UserDTO> listUsers() {
        LambdaQueryWrapper<User> wrapper = buildUserListQuery();
        wrapper.orderByAsc(User::getRoleId).orderByAsc(User::getId);
        List<User> list = super.list(wrapper);
        return list.stream().map(this::toUserDTO).collect(Collectors.toList());
    }

    @Override
    public Page<UserDTO> pageUsers(Integer current, Integer size, String username, String phone) {
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = buildUserListQuery();
        wrapper.like(StringUtils.hasText(username), User::getUsername, username);
        wrapper.like(StringUtils.hasText(phone), User::getPhone, phone);
        wrapper.orderByAsc(User::getRoleId).orderByAsc(User::getId);
        super.page(page, wrapper);
        return (Page<UserDTO>) page.convert(this::toUserDTO);
    }

    /**
     * 构建用户列表查询条件：
     * - 超级管理员：排除超级管理员自身，可看其他全部
     * - 普通管理员：只能看到自己 + 普通用户（CUSTOMER）
     */
    private LambdaQueryWrapper<User> buildUserListQuery() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser != null && "SUPER_ADMIN".equals(currentUser.getRoleCode())) {
            // 超级管理员：排除超级管理员自己
            Long superAdminRoleId = getSuperAdminRoleId();
            if (superAdminRoleId != null) {
                wrapper.ne(User::getRoleId, superAdminRoleId);
            }
        } else {
            // 普通管理员：能看到普通用户、商家 + 自己
            Long customerRoleId = getRoleIdByCode("CUSTOMER");
            Long merchantRoleId = getRoleIdByCode("MERCHANT");
            if (currentUser != null) {
                wrapper.and(w -> {
                    if (customerRoleId != null) {
                        w.eq(User::getRoleId, customerRoleId);
                    }
                    if (merchantRoleId != null) {
                        if (customerRoleId != null) {
                            w.or();
                        }
                        w.eq(User::getRoleId, merchantRoleId);
                    }
                    w.or().eq(User::getId, currentUser.getId());
                });
            } else {
                if (customerRoleId != null) {
                    wrapper.eq(User::getRoleId, customerRoleId);
                }
            }
        }
        return wrapper;
    }

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                dto.setRoleName(role.getName());
                dto.setRoleCode(role.getCode());
            }
        }
        return dto;
    }

    private User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return getOne(wrapper, false);
    }

    private User getByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return getOne(wrapper, false);
    }

    /**
     * 获取超级管理员角色的ID
     */
    private Long getSuperAdminRoleId() {
        return getRoleIdByCode("SUPER_ADMIN");
    }

    /**
     * 根据角色编码获取角色ID
     */
    private Long getRoleIdByCode(String code) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getCode, code);
        Role role = roleMapper.selectOne(wrapper, false);
        return role != null ? role.getId() : null;
    }

    /**
     * 检查指定用户是否为超级管理员，是则抛出异常
     */
    private void checkNotSuperAdmin(Long userId) {
        User user = super.getById(userId);
        if (user == null) return;
        Long superAdminRoleId = getSuperAdminRoleId();
        if (superAdminRoleId != null && superAdminRoleId.equals(user.getRoleId())) {
            throw new BusinessException(403, "不允许操作超级管理员账号");
        }
    }
}