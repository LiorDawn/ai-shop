package org.example.aishop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 1. 允许所有域名跨域（替换原来的指定域名）
        // Spring Boot 3 中 allowCredentials=true 时不能用 "*" 作为 Origin
        config.addAllowedOriginPattern("*");
        // 2. 允许携带Cookie/Token
        config.setAllowCredentials(true);
        // 3. 允许所有请求方法（GET/POST/PUT/DELETE等）
        config.addAllowedMethod("*");
        // 4. 允许所有请求头（包括Authorization/token）
        config.addAllowedHeader("*");
        // 5. 预检请求有效期（单位：秒），避免频繁OPTIONS请求
        config.setMaxAge(3600L);

        // 配置生效的URL路径（所有接口都允许跨域）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}