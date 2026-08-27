package org.example.aishop.interceptor;

import org.example.aishop.dto.UserDTO;
import org.example.aishop.util.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RolePermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 使用 servletPath 去掉 context-path (/api) 前缀
        String uri = request.getServletPath();

        UserDTO user = UserHolder.getUser();
        if (user == null) {
            response.setStatus(401);
            return false;
        }

        String roleCode = user.getRoleCode();

        // 超级管理员和管理员能访问所有
        if ("SUPER_ADMIN".equals(roleCode) || "ADMIN".equals(roleCode)) {
            return true;
        }

        // 商家只能访问 /shop/** 和 /merchant/**
        if ("MERCHANT".equals(roleCode) && (uri.startsWith("/shop/") || uri.startsWith("/merchant/"))) {
            return true;
        }

        // 普通用户可以访问入驻申请相关接口
        if ("CUSTOMER".equals(roleCode) && (uri.startsWith("/merchant/apply/") || uri.equals("/merchant/apply"))) {
            return true;
        }

        // 其余情况拒绝
        response.setStatus(403);
        response.getWriter().write("无权限");
        return false;
    }
}