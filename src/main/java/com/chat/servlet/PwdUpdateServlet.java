package com.chat.servlet;

import com.chat.util.DBUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/user/update-pwd")
public class PwdUpdateServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> params = mapper.readValue(request.getInputStream(), Map.class);
            String username = (String) params.get("username");
            String oldPwd = (String) params.get("oldPassword");
            String newPwd = (String) params.get("newPassword");

            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM user WHERE username=?");
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                result.put("code", 400);
                result.put("msg", "用户不存在");
            } else if (!BCrypt.checkpw(oldPwd, rs.getString("password"))) {
                result.put("code", 400);
                result.put("msg", "原密码错误");
            } else {
                String hashPwd = BCrypt.hashpw(newPwd, BCrypt.gensalt());
                pstmt = conn.prepareStatement("UPDATE user SET password=? WHERE username=?");
                pstmt.setString(1, hashPwd);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
                result.put("code", 200);
                result.put("msg", "密码修改成功");
            }
            DBUtil.close(conn, pstmt, rs);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "修改失败");
        }
        try {
            mapper.writeValue(response.getWriter(), result);
        } catch (Exception ignored) {}
    }
}
