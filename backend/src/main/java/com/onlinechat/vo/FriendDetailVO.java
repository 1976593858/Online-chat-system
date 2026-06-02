package com.onlinechat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "好友详情")
public class FriendDetailVO {

    private Long friendshipId;
    private Long friendId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Long groupId;
    private String groupName;
    private String remark;
    private LocalDateTime friendSince;
    private LocalDateTime lastLoginAt;
}
