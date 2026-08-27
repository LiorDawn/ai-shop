package org.example.aishop.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /** 支付宝 APPID（沙箱） */
    private String appId;

    /** 应用私钥（PKCS8 格式，需带 PEM 头） */
    private String privateKey;

    /** 支付宝公钥（验签用） */
    private String alipayPublicKey;

    /** 支付宝网关地址（沙箱） */
    private String gatewayUrl;

    /** 异步通知地址，需替换为公网可访问地址 */
    private String notifyUrl = "http://your-domain.com/api/pay/notify";

    /** 支付成功后同步跳转地址（会被 PayServiceImpl 动态拼接 orderId） */
    private String returnUrl = "http://localhost:5173/#/payment";

    /** 清理私钥：去头尾、去所有空白字符、去不可见字符，只保留 Base64 合法字符 */
    private String cleanPrivateKey() {
        if (privateKey == null) return "";
        String key = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "");
        // 只保留 Base64 合法字符（A-Za-z0-9+/=），过滤掉所有空白、不可见字符
        key = key.replaceAll("[^A-Za-z0-9+/=]", "");
        return key;
    }

    /** 将 Base64 私钥格式化为标准 PEM 格式（每行 64 字符） */
    private String toPemFormat(String base64Key) {
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PRIVATE KEY-----\n");
        int index = 0;
        while (index < base64Key.length()) {
            int end = Math.min(index + 64, base64Key.length());
            pem.append(base64Key, index, end).append("\n");
            index = end;
        }
        pem.append("-----END PRIVATE KEY-----");
        return pem.toString();
    }

    @PostConstruct
    public void validateKey() {
        System.out.println("===== 支付宝私钥格式深度验证 =====");
        System.out.println("appId: " + appId);
        System.out.println("gatewayUrl: " + gatewayUrl);
        System.out.println("notifyUrl: " + notifyUrl);
        System.out.println("returnUrl: " + returnUrl);

        if (privateKey == null || privateKey.trim().isEmpty()) {
            System.out.println("[错误] 私钥为空！请检查 application.yml alipay.private-key 配置");
            System.out.println("=================================");
            return;
        }

        System.out.println("私钥原始长度: " + privateKey.length());
        System.out.println("私钥原始前100字符: [" + privateKey.substring(0, Math.min(100, privateKey.length())) + "]");
        System.out.println("私钥原始后50字符: [" + privateKey.substring(Math.max(0, privateKey.length() - 50)) + "]");

        // 检查是否有不可见字符
        for (int i = 0; i < Math.min(20, privateKey.length()); i++) {
            char c = privateKey.charAt(i);
            if (Character.isWhitespace(c) && c != '\n' && c != '\r') {
                System.out.println("[警告] 私钥前20字符中发现空白字符(非换行): 位置=" + i + " 字符码=" + (int) c);
            }
        }

        try {
            // 清理私钥：移除所有空白字符
            String cleanKey = cleanPrivateKey();
            System.out.println("清理后Base64长度: " + cleanKey.length());

            // 检查清理后的字符串是否只包含Base64合法字符
            String base64Chars = cleanKey.replaceAll("[A-Za-z0-9+/=]", "");
            if (!base64Chars.isEmpty()) {
                System.out.println("[错误] 清理后仍存在非法字符: [" + base64Chars + "]");
                System.out.println("非法字符ASCII码: ");
                for (char c : base64Chars.toCharArray()) {
                    System.out.println("  '" + c + "' -> " + (int) c);
                }
            }

            // Base64解码
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
            System.out.println("解码后字节数: " + keyBytes.length);

            // 尝试用PKCS8解析
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey pk = keyFactory.generatePrivate(spec);
            System.out.println("私钥解析成功! 算法: " + pk.getAlgorithm() + " 格式: " + pk.getFormat());

            // 打印格式化后的PEM
            String pemKey = toPemFormat(cleanKey);
            System.out.println("格式化后PEM前50字符: [" + pemKey.substring(0, 50) + "]");
            System.out.println("格式化后PEM后50字符: [" + pemKey.substring(pemKey.length() - 50) + "]");

        } catch (IllegalArgumentException e) {
            System.out.println("Base64解码失败: " + e.getClass().getName());
            System.out.println("错误信息: " + e.getMessage());
            // 进一步诊断
            String cleanKey = cleanPrivateKey();
            System.out.println("清理后Base64长度: " + cleanKey.length());
            if (cleanKey.length() % 4 != 0) {
                System.out.println("[诊断] Base64长度不是4的倍数，可能缺少字符或多出字符");
            }
        } catch (Exception e) {
            System.out.println("私钥解析失败: " + e.getClass().getName());
            System.out.println("错误信息: " + e.getMessage());
            e.printStackTrace(System.out);
        }
        System.out.println("=================================");
    }

    @Bean
    public AlipayClient alipayClient() {
        // 直接传纯 Base64 私钥（不带 PEM 头），SDK 内部会自行处理
        String cleanKey = cleanPrivateKey();

        System.out.println("AlipayClient初始化: appId=" + appId + " gateway=" + gatewayUrl);
        System.out.println("私钥Base64长度: " + (cleanKey != null ? cleanKey.length() : 0));

        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                cleanKey,          // 纯 Base64，不带 PEM 头
                "json",
                "UTF-8",
                alipayPublicKey,
                "RSA2"
        );
    }
}