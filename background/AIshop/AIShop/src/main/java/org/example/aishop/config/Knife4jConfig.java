package org.example.aishop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI智能商城接口文档")
                        .description("AI智能购物商城系统 — 前后端分离接口文档<br/>"
                                + "<small>模块说明：</small><br/>"
                                + "<ul>"
                                + "<li><b>用户端</b> — 商品浏览、购物车、下单支付、AI 智能推荐、客服聊天</li>"
                                + "<li><b>商家端</b> — 店铺管理、商品管理、订单处理、售后审核、数据统计</li>"
                                + "<li><b>管理端</b> — 用户管理、商家审核、系统监控、全局配置</li>"
                                + "</ul>")
                        .contact(new Contact()
                                .name("AI Shop Team")
                                .email("admin@aishop.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .version("1.0.0"))
                // 全局鉴权 — 请求头携带 Token
                .components(new Components()
                        .addSecuritySchemes("Authorization", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("登录后获取的 Token，格式：<code>Bearer {token}</code>")))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));
    }

    @Bean
    public GroupedOpenApi commonApi() {
        return GroupedOpenApi.builder()
                .group("1-公共模块")
                .displayName("公共模块（商品/分类/文件）")
                .pathsToMatch("/api/product/**", "/api/category/**", "/api/file/**", "/api/upload/**", "/api/role/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("2-用户端")
                .displayName("用户端（认证/订单/支付/AI等）")
                .pathsToMatch("/api/user/**", "/api/order/**", "/api/pay/**", "/api/cart/**",
                        "/api/coupon/**", "/api/comment/**", "/api/afterSale/**", "/api/collect/**",
                        "/api/aiChat/**", "/api/customerChat/**")
                .build();
    }

    @Bean
    public GroupedOpenApi merchantApi() {
        return GroupedOpenApi.builder()
                .group("3-商家端")
                .displayName("商家端（店铺/订单/商品统计）")
                .pathsToMatch("/api/merchant/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("4-管理端")
                .displayName("管理端（用户/商家/系统管理）")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}