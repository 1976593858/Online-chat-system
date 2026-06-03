package com.onlinechat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
@Schema(description = "会话列表实体")
public class Conversation {

    @TableId
    private Long id;
    private Long ownerId;
    private Long targetUserId;
    private String conversationType;
    private Long lastMessageId;
    private String lastMessageContent;
    private String lastMessageType;
    private Integer unreadCount;
    private LocalDateTime lastMessageAt;
    private Integer pinned;
    private Integer muted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
