package com.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
@Schema(description = "更新群聊信息")
public class GroupUpdateDTO {

    @Size(min = 1, max = 64, message = "群名称长度应为1-64位")
    @Schema(description = "群名称", example = "全栈开发交流群")
    private String name;

    @Size(max = 500, message = "群公告不能超过500个字符")
    @Schema(description = "群公告", example = "欢迎大家交流技术问题")
    private String announcement;
}
