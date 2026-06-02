package com.onlinechat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "好友列表项")
public class FriendVO {

    private Long friendshipId;
    private Long friendId;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private Long groupId;
    private String groupName;
    private String remark;
    private LocalDateTime createdAt;
}
