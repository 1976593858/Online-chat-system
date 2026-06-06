package com.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "发送群消息请求")
public class SendMessageDTO {

    @NotBlank(message = "群ID不能为空")
    @Schema(description = "群ID", example = "1")
    private String groupId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    @Schema(description = "消息内容", example = "大家好")
    private String content;
}
