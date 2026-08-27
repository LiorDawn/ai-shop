package org.example.aishop.service.user;

public interface SmsService {

    /**
     * 发送验证码到手机
     */
    void sendCode(String phone);

    /**
     * 发送验证码到邮箱
     */
    void sendEmailCode(String email);

    /**
     * 通用发送验证码
     * @param account 手机号或邮箱
     * @param type 1=手机, 2=邮箱
     */
    void sendCode(String account, Integer type);

    /**
     * 校验手机验证码
     */
    boolean validateCode(String phone, String code);

    /**
     * 校验邮箱验证码
     */
    boolean validateEmailCode(String email, String code);

    /**
     * 通用校验验证码
     * @param account 手机号或邮箱
     * @param code 验证码
     * @param type 1=手机, 2=邮箱
     * @return true 验证通过
     */
    boolean validateCode(String account, String code, Integer type);
}