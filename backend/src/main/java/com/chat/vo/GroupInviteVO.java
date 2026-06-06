package com.chat.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupInviteVO {
    private Long id;
    private Long groupId;
    private String groupName;
    private Long senderId;
    private String senderUsername;
    private String senderNickname;
    private String status;
    private LocalDateTime createdAt;
}
