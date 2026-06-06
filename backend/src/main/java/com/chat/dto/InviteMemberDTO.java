package com.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "邀请成员请求")
public class InviteMemberDTO {
    @NotNull(message = "被邀请人ID不能为空")
    @Schema(description = "被邀请人用户ID")
    private Long inviteeId;
}
