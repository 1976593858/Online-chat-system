package com.chat.servlet;

import com.chat.entity.User;
import com.chat.util.DBUtil;
import com.chat.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        User user = mapper.readValue(request.getInputStream(), User.class);
        Map<String, Object> result = new HashMap<>();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement("SELECT id,password,nickname,avatar FROM user WHERE username=?");
            pstmt.setString(1, user.getUsername());
            rs = pstmt.executeQuery();

            if (!rs.next()) {
                result.put("code", 401);
                result.put("msg", "用户不存在");
            } else if (!BCrypt.checkpw(user.getPassword(), rs.getString("password"))) {
                result.put("code", 401);
                result.put("msg", "密码错误");
            } else {
                Integer uid = rs.getInt("id");
                String token = JwtUtil.generateToken(uid, user.getUsername());
                Map<String, Object> info = new HashMap<>();
                info.put("userId", uid);
                info.put("username", user.getUsername());
                info.put("nickname", rs.getString("nickname"));
                info.put("avatar", rs.getString("avatar"));

                result.put("code", 200);
                result.put("msg", "登录成功");
                result.put("token", token);
                result.put("user", info);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "服务器错误");
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        mapper.writeValue(response.getWriter(), result);
    }
}