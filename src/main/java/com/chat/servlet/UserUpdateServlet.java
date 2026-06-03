package com.chat.servlet;

import com.chat.util.DBUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/user/update")
public class UserUpdateServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        Integer userId = (Integer) request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> params = mapper.readValue(request.getInputStream(), Map.class);
            String nickname = (String) params.get("nickname");
            String phone = (String) params.get("phone");

            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE user SET nickname=?, phone=? WHERE id=?");
            pstmt.setString(1, nickname);
            pstmt.setString(2, phone);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();

            result.put("code", 200);
            result.put("msg", "保存成功");
            DBUtil.close(conn, pstmt, null);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "保存失败");
        }
        try {
            mapper.writeValue(response.getWriter(), result);
        } catch (Exception ignored) {}
    }
}