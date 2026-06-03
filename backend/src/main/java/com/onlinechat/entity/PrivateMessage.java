package com.onlinechat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("private_message")
@Schema(description = "私聊消息实体")
public class PrivateMessage {

    @TableId
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String messageType;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}

