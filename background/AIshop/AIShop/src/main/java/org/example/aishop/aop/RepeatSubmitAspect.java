package org.example.aishop.aop;

import org.example.aishop.annotation.RepeatSubmit;
import org.example.aishop.common.exception.DuplicateSubmitException;
import org.example.aishop.util.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * 防重复提交切面
 * 基于 Redisson 分布式锁，对同一用户在同一接口的相同参数请求进行拦截
 * 锁 key 格式：repeat:submit:{userId}:{uri}:{paramMd5}
 */
@Aspect
@Component
public class RepeatSubmitAspect {

    private static final Logger log = LoggerFactory.getLogger(RepeatSubmitAspect.class);

    private final RedissonClient redissonClient;

    public RepeatSubmitAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        String lockKey = buildLockKey(joinPoint, repeatSubmit);
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = lock.tryLock(repeatSubmit.waitTime(), repeatSubmit.leaseTime(), repeatSubmit.timeUnit());
        if (!acquired) {
            log.warn("重复提交被拦截: key=" + lockKey);
            throw new DuplicateSubmitException(repeatSubmit.message());
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String buildLockKey(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 用户 ID
        String userId = "anon";
        try {
            Long uid = UserHolder.getUserId();
            if (uid != null) userId = String.valueOf(uid);
        } catch (Exception ignored) {}

        // 请求 URI
        String uri = "";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                uri = request.getRequestURI();
            }
        } catch (Exception ignored) {}

        // 参数 MD5（排除 HttpServletRequest/Response 等不可序列化类型）
        StringBuilder paramStr = new StringBuilder();
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg == null) continue;
                if (arg instanceof HttpServletRequest) continue;
                if (arg instanceof jakarta.servlet.http.HttpServletResponse) continue;
                try {
                    paramStr.append(arg.toString());
                } catch (Exception ignored) {}
            }
        }
        String paramMd5 = DigestUtils.md5DigestAsHex(paramStr.toString().getBytes(StandardCharsets.UTF_8));

        return repeatSubmit.prefix() + userId + ":" + uri + ":" + paramMd5;
    }
}