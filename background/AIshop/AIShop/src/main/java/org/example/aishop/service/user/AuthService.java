package org.example.aishop.service.user;

import org.example.aishop.dto.LoginVO;
import org.example.aishop.dto.RegisterDTO;

public interface AuthService {

    /**
     * 发送验证码（手机）
     */
    void sendSmsCode(String phone);

    /**
     * 通用发送验证码
     */
    void sendCode(String account, Integer type);

    /**
     * 密码登录（通用：手机号或邮箱）
     */
    LoginVO loginByPassword(String account, String password, Integer type);

    /**
     * 密码登录（手机号）
     */
    LoginVO loginByPassword(String phone, String password);

    /**
     * 验证码登录
     */
    LoginVO loginBySms(String phone, String code);

    /**
     * 普通用户注册（手机号）
     */
    LoginVO registerUser(RegisterDTO dto);

    /**
     * 验证码注册（通用：手机或邮箱）
     */
    LoginVO registerByCode(String account, String code, String password, Integer type);

    /**
     * 重置密码
     */
    void resetPassword(String account, String code, String newPwd, Integer type);
}