package org.example.aishop.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.common.result.Result;
import org.example.aishop.dto.*;
import org.example.aishop.service.user.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户认证")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/sendCode")
    @Operation(summary = "发送验证码（手机/邮箱）")
    public Result<Void> sendCode(@RequestBody SendCodeDTO dto) {
        authService.sendCode(dto.getAccount(), dto.getType());
        return Result.success("验证码已发送");
    }

    @PostMapping("/loginByPwd")
    @Operation(summary = "账号密码登录（手机/邮箱）")
    public Result<LoginVO> loginByPwd(@RequestBody LoginByPwdDTO dto) {
        LoginVO loginVO = authService.loginByPassword(dto.getAccount(), dto.getPassword(), dto.getType());
        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/register")
    @RepeatSubmit(prefix = "repeat:submit:register:", leaseTime = 5, message = "注册请求正在处理中，请勿重复提交")
    @Operation(summary = "验证码注册（手机/邮箱）")
    public Result<LoginVO> register(@RequestBody RegisterByCodeDTO dto) {
        LoginVO loginVO = authService.registerByCode(dto.getAccount(), dto.getCode(), dto.getPassword(), dto.getType());
        return Result.success("注册成功", loginVO);
    }

    @PostMapping("/resetPwd")
    @Operation(summary = "重置密码（手机/邮箱）")
    public Result<Void> resetPwd(@RequestBody ResetPwdDTO dto) {
        authService.resetPassword(dto.getAccount(), dto.getCode(), dto.getNewPwd(), dto.getType());
        return Result.success("密码重置成功");
    }
}