package com.chat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_message")
public class GroupMessage {
    @TableId
    private Long id;
    private Long groupId;
    private Long fromUserId;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
