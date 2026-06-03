package com.onlinechat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Schema(description = "发送私聊消息请求")
public class PrivateMessageSendDTO {

    @NotNull(message = "接收人ID不能为空")
    @Schema(description = "接收人用户ID", example = "2")
    private Long toUserId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    @Schema(description = "消息内容", example = "你好")
    private String content;

    @Schema(description = "消息类型：TEXT、IMAGE、VOICE、FILE", example = "TEXT")
    private String messageType;
}

