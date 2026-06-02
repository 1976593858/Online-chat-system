package com.onlinechat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "好友分组")
public class FriendGroupVO {

    private Long id;
    private String name;
    private Integer isDefault;
    private Integer sortOrder;
    private Long friendCount;
    private LocalDateTime createdAt;
}
