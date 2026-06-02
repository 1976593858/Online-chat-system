package com.onlinechat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
@Schema(description = "修改好友备注请求")
public class FriendRemarkDTO {

    @Size(max = 50, message = "好友备注不能超过50个字符")
    @Schema(description = "好友备注", example = "项目搭档")
    private String remark;
}
