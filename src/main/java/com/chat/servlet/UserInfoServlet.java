package com.chat.servlet;

import com.chat.util.DBUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/user/info")
public class UserInfoServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        Integer userId = (Integer) request.getAttribute("userId");

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement("SELECT username,nickname,phone,avatar FROM user WHERE id=?");
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> data = new HashMap<>();
                data.put("userId", userId);
                data.put("username", rs.getString("username"));
                data.put("nickname", rs.getString("nickname"));
                data.put("phone", rs.getString("phone"));
                data.put("avatar", rs.getString("avatar"));
                result.put("code", 200);
                result.put("data", data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mapper.writeValue(response.getWriter(), result);
        } catch (Exception ignored) {}
    }
}