package com.onlinechat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "移动好友分组请求")
public class FriendMoveDTO {

    @NotNull(message = "目标分组ID不能为空")
    @Schema(description = "目标分组ID", example = "2")
    private Long groupId;
}
