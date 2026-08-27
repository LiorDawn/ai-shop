package org.example.aishop.controller.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.common.result.Result;
import org.example.aishop.util.FileUploadUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 文件上传控制器（含 Redis 限流）
 * </p>
 */
@Tag(name = "文件上传", description = "图片上传（含 Redis IP 限流）")
@RestController
@RequestMapping("/upload")
public class UploadController {

    @Resource
    private FileUploadUtil fileUploadUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "上传图片", description = "支持商品图/头像等，Redis 限流（同 IP 每分钟 10 次）")
    @PostMapping("/image")
    public Result upload(@RequestParam("file") MultipartFile file,
                         @RequestParam String imageType,
                         HttpServletRequest request) throws IOException {

        // ===== Redis 限流：同一 IP 每分钟最多 10 次上传 =====
        String clientIp = getClientIp(request);
        String rateLimitKey = RedisConstant.uploadRateLimitKey(clientIp);

        Long currentCount = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (currentCount != null && currentCount == 1) {
            // 首次请求，设置过期时间
            stringRedisTemplate.expire(rateLimitKey, RedisConstant.UPLOAD_RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        }
        if (currentCount != null && currentCount > RedisConstant.UPLOAD_RATE_LIMIT_MAX) {
            return Result.fail("上传过于频繁，请稍后再试");
        }

        try {
            String fileUrl = fileUploadUtil.uploadImage(file, imageType);
            return Result.success("上传成功", fileUrl);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}