package com.chat.filter;

import com.chat.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/api/*")
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getRequestURI();
        if (path.contains("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            resp.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return;
        }

        if (!JwtUtil.validateToken(auth)) {
            resp.getWriter().write("{\"code\":401,\"msg\":\"登录已过期\"}");
            return;
        }

        Integer userId = JwtUtil.getUserIdFromToken(auth);
        req.setAttribute("userId", userId);
        chain.doFilter(request, response);
    }
}