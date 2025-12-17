package com.zootopia.interceptor;

import com.zootopia.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行OPTIONS请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取请求路径（去掉 context-path 后的路径）
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            // 如果 servletPath 为空，尝试从 RequestURI 中提取
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (uri.startsWith(contextPath)) {
                path = uri.substring(contextPath.length());
            } else {
                path = uri;
            }
        }
        
        // 放行登录、注册和健康检查接口（这些路径应该已经在 WebConfig 中排除了，但这里再加一层检查）
        if ("/login".equals(path) || "/register".equals(path) || "/health".equals(path) ||
            path.endsWith("/login") || path.endsWith("/register") || path.endsWith("/health")) {
            return true;
        }
        
        // 放行商品图片接口（公开访问）
        if (path.contains("/shop/products/") && path.endsWith("/image")) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            return false;
        }

        token = token.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            String role = claims.get("role", String.class);
            
            // 检查角色权限（这里简化处理，具体权限检查在Controller中）
            request.setAttribute("userId", claims.get("id", Integer.class));
            request.setAttribute("userRole", role);
            request.setAttribute("username", claims.get("username", String.class));
            request.setAttribute("userName", claims.get("name", String.class));
            
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }
    }
}

