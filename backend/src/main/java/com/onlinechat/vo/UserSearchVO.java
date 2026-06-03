package com.onlinechat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户搜索结果")
public class UserSearchVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    @Schema(description = "关系状态：SELF、FRIEND、NONE、PENDING_SENT、PENDING_RECEIVED")
    private String relationStatus;
}
