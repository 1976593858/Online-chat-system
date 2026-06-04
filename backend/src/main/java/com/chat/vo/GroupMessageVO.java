package com.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMessageVO {
    private Long id;
    private Long groupId;
    private Long fromUserId;
    private String fromUsername;
    private String fromNickname;
    private String fromAvatar;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;
}
