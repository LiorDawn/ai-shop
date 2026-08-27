package org.example.aishop.config;

import org.example.aishop.interceptor.CorsInterceptor;
import org.example.aishop.interceptor.LoginInterceptor;
import org.example.aishop.interceptor.RolePermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1. 跨域拦截器（最先执行）
        registry.addInterceptor(new CorsInterceptor())
                .addPathPatterns("/**");

        // 2. 登录拦截器
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                                "/auth/**",
                                "/user/sendCode",
                                "/user/loginByPwd",
                                "/user/register",
                                "/user/resetPwd",
                                "/user/login",
                                "/user/register",
                                "/captcha/**",
                                "/product/page",
                                "/product/hot",
                                "/product/list",
                                "/product/recommend",
                                "/category/**",
                                "/upload/**",
                                "/ws/**",
                                "/pay/notify",
                                "/doc.html",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        );

        // 3. 角色权限拦截器
        registry.addInterceptor(new RolePermissionInterceptor())
                .addPathPatterns("/admin/**", "/merchant/**");
    }
}