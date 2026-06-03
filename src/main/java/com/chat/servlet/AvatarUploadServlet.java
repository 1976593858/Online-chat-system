package com.chat.servlet;

import com.chat.util.DBUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 头像上传接口（兼容 Tomcat 10+ jakarta.servlet）
 */
@WebServlet("/api/user/upload-avatar")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024, // 1MB
        maxFileSize = 1024 * 1024 * 5, // 5MB
        maxRequestSize = 1024 * 1024 * 10 // 10MB
)
public class AvatarUploadServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        Integer userId = (Integer) request.getAttribute("userId");

        // 1. 检查用户是否登录
        if (userId == null) {
            result.put("code", 401);
            result.put("msg", "未登录，请先登录");
            mapper.writeValue(response.getWriter(), result);
            return;
        }

        try {
            // 2. 获取上传的文件（前端表单字段名必须是 "avatar"）
            Part part = request.getPart("avatar");
            if (part == null || part.getSize() == 0) {
                result.put("code", 400);
                result.put("msg", "请选择要上传的图片");
                mapper.writeValue(response.getWriter(), result);
                return;
            }

            // 3. 校验文件类型（只允许图片）
            String contentType = part.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("code", 400);
                result.put("msg", "请上传图片文件（jpg/png/gif）");
                mapper.writeValue(response.getWriter(), result);
                return;
            }

            // 4. 生成保存路径
            String uploadDir = getServletContext().getRealPath("/uploads");
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs(); // 创建目录
            }

            // 5. 生成唯一文件名（防止重名覆盖）
            String fileName = getFileName(part);
            String suffix = fileName.substring(fileName.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;

            // 6. 保存文件到服务器
            File targetFile = new File(uploadDirFile, newFileName);
            part.write(targetFile.getAbsolutePath());

            // 7. 更新数据库中的头像路径
            String avatarUrl = "/uploads/" + newFileName;
            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("UPDATE user SET avatar = ? WHERE id = ?");
            pstmt.setString(1, avatarUrl);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            DBUtil.close(conn, pstmt, null);

            // 8. 返回成功结果
            result.put("code", 200);
            result.put("msg", "上传成功");
            result.put("avatar", avatarUrl);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("msg", "上传失败：" + e.getMessage());
        }

        mapper.writeValue(response.getWriter(), result);
    }

    /**
     * 从 Part 中获取原始文件名
     */
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        String[] items = contentDisposition.split(";");
        for (String item : items) {
            if (item.trim().startsWith("filename")) {
                return item.substring(item.indexOf("=") + 2, item.length() - 1);
            }
        }
        return "unknown";
    }
}