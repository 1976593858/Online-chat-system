package com.onlinechat.vo;

import java.time.LocalDateTime;

public class ChatHistorySearchVO {

    private Long id;
    private Long messageId;
    private Long fromUserId;
    private String fromUserNickname;
    private String fromUserUsername;
    private Long toUserId;
    private String toUserNickname;
    private String toUserUsername;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUserNickname() { return fromUserNickname; }
    public void setFromUserNickname(String fromUserNickname) { this.fromUserNickname = fromUserNickname; }

    public String getFromUserUsername() { return fromUserUsername; }
    public void setFromUserUsername(String fromUserUsername) { this.fromUserUsername = fromUserUsername; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

    public String getToUserNickname() { return toUserNickname; }
    public void setToUserNickname(String toUserNickname) { this.toUserNickname = toUserNickname; }

    public String getToUserUsername() { return toUserUsername; }
    public void setToUserUsername(String toUserUsername) { this.toUserUsername = toUserUsername; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
