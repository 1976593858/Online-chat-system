package com.onlinechat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
@Schema(description = "处理好友申请请求")
public class FriendRequestHandleDTO {

    @Schema(description = "同意后放入的分组ID；为空时放入默认分组", example = "1")
    private Long groupId;

    @Size(max = 50, message = "好友备注不能超过50个字符")
    @Schema(description = "好友备注", example = "同学")
    private String remark;

    @Size(max = 200, message = "处理原因不能超过200个字符")
    @Schema(description = "拒绝原因", example = "暂不添加")
    private String handleReason;
}
