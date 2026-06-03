package com.onlinechat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_request")
@Schema(description = "好友申请实体")
public class FriendRequest {

    @TableId
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String message;
    private String status;
    private String handleReason;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
