package org.example.aishop.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送邮件验证码
     *
     * @param to      收件人邮箱
     * @param code    6位验证码
     */
    public void sendVerificationCode(String to, String code) {
        if (mailSender == null) {
            System.out.println("===== 邮件验证码（邮件服务未配置，打印到控制台） =====");
            System.out.println("收件人: " + to);
            System.out.println("验证码: " + code);
            System.out.println("================================================");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("AI智能商城 - 验证码");
            helper.setText("<div style=\"max-width:600px;margin:0 auto;padding:20px;font-family:Arial,sans-serif;border:1px solid #eee;border-radius:8px;\">" +
                    "<h2 style=\"color:#333;\">AI智能商城</h2>" +
                    "<p style=\"font-size:14px;color:#666;\">您的验证码为：</p>" +
                    "<p style=\"font-size:32px;font-weight:bold;color:#ff0f23;letter-spacing:8px;text-align:center;padding:15px 0;\">" + code + "</p>" +
                    "<p style=\"font-size:12px;color:#999;\">验证码5分钟内有效，请勿泄露给他人。</p>" +
                    "<hr style=\"border:none;border-top:1px solid #eee;\">" +
                    "<p style=\"font-size:12px;color:#ccc;\">此邮件由系统自动发送，请勿回复</p>" +
                    "</div>", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }
}