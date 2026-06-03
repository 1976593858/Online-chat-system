package com.onlinechat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Schema(description = "注册请求")
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名只能包含字母、数字、下划线，长度4-32位")
    @Schema(description = "用户名", example = "alice")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称不能超过32个字符")
    @Schema(description = "昵称", example = "Alice")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "alice@example.com")
    private String email;

    @Size(max = 20, message = "手机号不能超过20个字符")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度应为6-64位")
    @Schema(description = "密码", example = "123456")
    private String password;
}
