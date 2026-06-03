package com.onlinechat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "JWT令牌")
    private String token;

    @Builder.Default
    @Schema(description = "令牌类型")
    private String tokenType = "Bearer";

    @Schema(description = "过期秒数")
    private Long expiresIn;

    @Schema(description = "登录用户")
    private UserVO user;
}
