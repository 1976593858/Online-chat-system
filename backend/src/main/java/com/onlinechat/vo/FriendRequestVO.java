package com.onlinechat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "好友申请")
public class FriendRequestVO {

    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderNickname;
    private String senderAvatar;
    private Long receiverId;
    private String receiverUsername;
    private String receiverNickname;
    private String receiverAvatar;
    private String message;
    private String status;
    private String handleReason;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
