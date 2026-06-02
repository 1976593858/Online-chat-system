package com.onlinechat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Schema(description = "创建好友申请请求")
public class FriendRequestCreateDTO {

    @NotNull(message = "接收人ID不能为空")
    @Schema(description = "接收人用户ID", example = "2")
    private Long receiverId;

    @Size(max = 200, message = "申请备注不能超过200个字符")
    @Schema(description = "申请备注", example = "我是 Alice")
    private String message;
}
