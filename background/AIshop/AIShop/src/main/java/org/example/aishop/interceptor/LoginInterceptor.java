package org.example.aishop.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aishop.common.constant.RedisConstant;
import org.example.aishop.dto.UserDTO;
import org.example.aishop.util.JwtUtil;
import org.example.aishop.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.concurrent.TimeUnit;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,Object handler) throws Exception{
        //请求头里拿到token
        String token=request.getHeader("token");
        if(token==null||token.trim().isEmpty()){
            response.setStatus(401);
            response.getWriter().write("未登录，请先登录");
            return false;
        }
        //拿到token校验合法性
        if(!jwtUtil.validateToken(token)){
            response.setStatus(401);
            response.getWriter().write("token无效");
            return false;
        }
        //校验是否过期
        if(jwtUtil.isTokenExpired(token)){
            response.setStatus(401);
            response.getWriter().write("token已过期");
            return false;
        }
        //解析token
        String userIdStr= jwtUtil.getUserIdFromToken(token);
        if (userIdStr == null){
            response.setStatus(401);
            response.getWriter().write("token解析失败");
            return false;
        }

        // 1. 读取 Redis 中该用户的最新 token 并比对
        Long userId = Long.parseLong(userIdStr);
        String storedToken = stringRedisTemplate.opsForValue().get(RedisConstant.userTokenKey(userId));
        if (storedToken == null) {
            response.setStatus(401);
            response.getWriter().write("登录已失效，请重新登录");
            return false;
        }
        if (!storedToken.equals(token)) {
            response.setStatus(401);
            response.getWriter().write("账号已在其他设备登录，请重新登录");
            return false;
        }

        // 2. 读取用户信息缓存
        UserDTO dto = null;
        String cachedJson = stringRedisTemplate.opsForValue().get(RedisConstant.userInfoKey(userId));
        if (cachedJson != null) {
            try {
                dto = objectMapper.readValue(cachedJson, UserDTO.class);
            } catch (Exception ignored) {}
        }
        if (dto == null) {
            response.setStatus(401);
            response.getWriter().write("登录信息异常，请重新登录");
            return false;
        }

        // 3. 延长 Redis 缓存 TTL
        try {
            stringRedisTemplate.expire(RedisConstant.userTokenKey(userId),
                    RedisConstant.USER_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.expire(RedisConstant.userInfoKey(userId),
                    RedisConstant.USER_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        UserHolder.saveUser(dto);
        return true;
    }
    // 请求结束后清空，防止内存泄漏
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserHolder.removeUser();
    }
}
