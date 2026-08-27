package org.example.aishop.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aishop.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "系统配置", description = "AI 审核开关等全局配置")
@RestController
@RequestMapping("/admin/system-config")
public class SystemConfigController {

    private static final String AI_REVIEW_ENABLED_KEY = "AISHOP:CONFIG:AI_REVIEW_ENABLED";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Operation(summary = "获取 AI 审核开关状态")
    @GetMapping("/ai-review")
    public Result<Map<String, Boolean>> getAiReviewStatus() {
        String val = redisTemplate.opsForValue().get(AI_REVIEW_ENABLED_KEY);
        boolean enabled = !"false".equals(val); // 默认开启
        Map<String, Boolean> result = new HashMap<>();
        result.put("enabled", enabled);
        return Result.success("查询成功", result);
    }

    @Operation(summary = "设置 AI 审核开关")
    @PutMapping("/ai-review")
    public Result<Map<String, Boolean>> setAiReviewStatus(@RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) return Result.fail("参数错误");
        redisTemplate.opsForValue().set(AI_REVIEW_ENABLED_KEY, String.valueOf(enabled), 30, TimeUnit.DAYS);
        Map<String, Boolean> result = new HashMap<>();
        result.put("enabled", enabled);
        return Result.success("设置成功", result);
    }
}