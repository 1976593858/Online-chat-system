package com.onlinechat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "创建好友分组请求")
public class FriendGroupCreateDTO {

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 32, message = "分组名称不能超过32个字符")
    @Schema(description = "分组名称", example = "同学")
    private String name;
}
