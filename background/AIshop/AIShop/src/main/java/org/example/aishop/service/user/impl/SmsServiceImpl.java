package org.example.aishop.service.user.impl;

import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.common.exception.BusinessException;
import org.example.aishop.service.user.SmsService;
import org.example.aishop.util.EmailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements SmsService {

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private EmailUtil emailUtil;

    private static final Random RANDOM = new Random();

    @Override
    public void sendCode(String phone) {
        sendCode(phone, 1);
    }

    @Override
    public void sendEmailCode(String email) {
        sendCode(email, 2);
    }

    @Override
    public void sendCode(String account, Integer type) {
        if (type == 1) {
            // 手机校验
            if (account == null || !account.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(400, "手机号格式不正确");
            }
        } else if (type == 2) {
            // 邮箱校验
            if (account == null || !account.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
                throw new BusinessException(400, "邮箱格式不正确");
            }
        } else {
            throw new BusinessException(400, "验证码类型不正确");
        }

        // ===== Redis 限流校验 =====
        String limitKey = (type == 1) ? RedisConstant.SMS_LIMIT_PREFIX + account : RedisConstant.EMAIL_LIMIT_PREFIX + account;

        if (stringRedisTemplate != null) {
            String countStr = stringRedisTemplate.opsForValue().get(limitKey);
            int count = 0;
            if (countStr != null) {
                count = Integer.parseInt(countStr);
            }
            if (count >= RedisConstant.LIMIT_MAX_COUNT) {
                throw new BusinessException(429, "发送过于频繁，请5分钟后再试");
            }

            // 递增发送次数
            Long newCount = stringRedisTemplate.opsForValue().increment(limitKey);
            if (newCount != null && newCount == 1) {
                // 首次设置过期时间
                stringRedisTemplate.expire(limitKey, RedisConstant.LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
            }
        }

        // ===== 生成6位验证码 =====
        String code = String.format("%06d", RANDOM.nextInt(999999));

        // ===== Redis 缓存验证码 =====
        String codeKey;
        if (type == 1) {
            codeKey = RedisConstant.SMS_CODE_PREFIX + account;
        } else {
            codeKey = RedisConstant.EMAIL_CODE_PREFIX + account;
        }

        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set(codeKey, code,
                    RedisConstant.VERIFY_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        // ===== 发送验证码 =====
        if (type == 1) {
            // 短信发送（模拟）
            System.out.println("===== 短信验证码 =====");
            System.out.println("手机号: " + account);
            System.out.println("验证码: " + code);
            System.out.println("有效期: 5分钟");
            System.out.println("======================");
        } else {
            // 邮件发送
            emailUtil.sendVerificationCode(account, code);
        }
    }

    @Override
    public boolean validateCode(String phone, String code) {
        return validateCode(phone, code, 1);
    }

    @Override
    public boolean validateEmailCode(String email, String code) {
        return validateCode(email, code, 2);
    }

    @Override
    public boolean validateCode(String account, String code, Integer type) {
        String codeKey;
        if (type == 1) {
            codeKey = RedisConstant.SMS_CODE_PREFIX + account;
        } else if (type == 2) {
            codeKey = RedisConstant.EMAIL_CODE_PREFIX + account;
        } else {
            return false;
        }

        if (stringRedisTemplate == null) {
            return false;
        }

        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            return false;
        }

        boolean valid = savedCode.equals(code);
        if (valid) {
            // 验证通过后删除验证码（一次性使用）
            stringRedisTemplate.delete(codeKey);
        }
        return valid;
    }
}