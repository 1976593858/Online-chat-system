package com.chat.servlet;

import com.chat.entity.User;
import com.chat.util.DBUtil;
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

@WebServlet("/api/auth/register")
public class RegisterServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        User user = mapper.readValue(request.getInputStream(), User.class);

        if (user.getUsername() == null || user.getUsername().length() < 3 || user.getPassword() == null || user.getPassword().length() < 6) {
            result.put("code", 400);
            result.put("msg", "用户名≥3位，密码≥6位");
            mapper.writeValue(response.getWriter(), result);
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement("SELECT id FROM user WHERE username=?");
            pstmt.setString(1, user.getUsername());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                result.put("code", 400);
                result.put("msg", "用户名已存在");
                mapper.writeValue(response.getWriter(), result);
                return;
            }

            String pwd = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            pstmt = conn.prepareStatement("INSERT INTO user(username,password,nickname,phone) VALUES(?,?,?,?)");
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, pwd);
            pstmt.setString(3, user.getNickname() == null ? user.getUsername() : user.getNickname());
            pstmt.setString(4, user.getPhone());
            pstmt.executeUpdate();

            result.put("code", 200);
            result.put("msg", "注册成功");
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
