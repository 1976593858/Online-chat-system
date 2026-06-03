package com.onlinechat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "私聊消息")
public class PrivateMessageVO {

    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String messageType;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}

