package com.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "群免打扰设置请求")
public class GroupMuteDTO {
    @NotNull(message = "muted不能为空")
    @Schema(description = "是否免打扰")
    private Boolean muted;
}
