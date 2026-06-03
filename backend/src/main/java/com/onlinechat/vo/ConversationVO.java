package com.onlinechat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "最近会话")
public class ConversationVO {

    private Long id;
    private Long targetUserId;
    private String targetUsername;
    private String targetNickname;
    private String targetAvatar;
    private String conversationType;
    private Long lastMessageId;
    private String lastMessageContent;
    private String lastMessageType;
    private Integer unreadCount;
    private Integer pinned;
    private Integer muted;
    private LocalDateTime lastMessageAt;
}
