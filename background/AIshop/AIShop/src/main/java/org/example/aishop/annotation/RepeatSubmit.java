package org.example.aishop.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交注解
 * 基于 Redisson 分布式锁 + 请求参数 MD5 实现接口幂等
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /** 锁等待时间，默认不等待，拿不到锁直接拒绝 */
    long waitTime() default 0;

    /** 锁持有时间 */
    long leaseTime() default 3;

    /** 时间单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** 幂等 key 前缀 */
    String prefix() default "repeat:submit:";

    /** 提示消息 */
    String message() default "请勿重复提交，请稍后再试";
}