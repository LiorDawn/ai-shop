package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aishop.common.result.Result;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.entity.user.Role;
import org.example.aishop.entity.user.User;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.service.user.RoleService;
import org.example.aishop.service.user.UserService;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户中心", description = "当前用户信息获取、角色管理（通用层）")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    /**
     * 获取当前登录用户最新信息（从 Redis 缓存读取，角色变更后立即生效）
     */
    @GetMapping("/current")
    public Result<UserDTO> current() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException(401, "未登录");
        }
        return Result.success("查询成功", user);
    }

    @PostMapping
    public Result<Void> add(@RequestBody User user) {
        // 只有 SUPER_ADMIN 才能创建管理员角色的用户
        if (user.getRoleId() != null) {
            Role role = roleService.getById(user.getRoleId());
            if (role != null && ("SUPER_ADMIN".equals(role.getCode()) || "ADMIN".equals(role.getCode()))) {
                UserDTO currentUser = UserHolder.getUser();
                if (currentUser == null || !"SUPER_ADMIN".equals(currentUser.getRoleCode())) {
                    throw new BusinessException(403, "仅超级管理员可添加管理员用户");
                }
            }
        }
        userService.addUser(user);
        return Result.success("添加成功");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        userService.deleteBatchUsers(ids);
        return Result.success("批量删除成功");
    }

    @Operation(summary = "更新用户")
    @PutMapping
    public Result<Void> update(@RequestBody User user) {
        userService.updateUser(user);
        return Result.success("修改成功");
    }

    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.success("状态更新成功");
    }

    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable Long id) {
        UserDTO dto = userService.getUserById(id);
        return Result.success("查询成功", dto);
    }

    @GetMapping("/list")
    public Result<List<UserDTO>> list() {
        List<UserDTO> list = userService.listUsers();
        return Result.success("查询成功", list);
    }

    @Operation(summary = "用户分页查询")
    @GetMapping("/page")
    public Result<Page<UserDTO>> page(@RequestParam(defaultValue = "1") Integer current,
                                      @RequestParam(defaultValue = "10") Integer size,
                                      @RequestParam(required = false) String username,
                                      @RequestParam(required = false) String phone) {
        Page<UserDTO> page = (Page<UserDTO>) userService.pageUsers(current, size, username, phone);
        return Result.success("查询成功", page);
    }
}