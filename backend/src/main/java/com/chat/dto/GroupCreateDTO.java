package com.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "创建群聊请求")
public class GroupCreateDTO {
    @NotBlank
    @Size(min = 1, max = 64)
    @Schema(description = "群名称", example = "技术交流群")
    private String name;

    @Size(max = 500)
    @Schema(description = "群公告", example = "欢迎大家")
    private String announcement;
}
