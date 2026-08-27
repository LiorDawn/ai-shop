package org.example.aishop.controller.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.common.result.Result;
import org.example.aishop.entity.user.Role;
import org.example.aishop.service.user.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理", description = "角色查询（用户/商家/管理员）")
@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Operation(summary = "角色列表")
    @GetMapping("/list")
    public Result<List<Role>> list() {
        List<Role> list = roleService.list();
        return Result.success("查询成功", list);
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Long id) {
        Role role = roleService.getById(id);
        return Result.success("查询成功", role);
    }
}