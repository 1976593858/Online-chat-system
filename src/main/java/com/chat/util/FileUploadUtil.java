package com.chat.util;

import org.apache.commons.fileupload.FileItem;
import java.util.UUID;

public class FileUploadUtil {
    public static String getFileSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public static String getRandomName() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isImage(FileItem item) {
        String contentType = item.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
}