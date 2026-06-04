package com.onlinechat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({"com.onlinechat.mapper", "com.chat.mapper"})
@SpringBootApplication(scanBasePackages = {"com.onlinechat", "com.chat"})
public class OnlineChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineChatApplication.class, args);
    }
}
