package com.onlinechat.controller;

import com.onlinechat.common.Result;
import com.onlinechat.dto.LoginDTO;
import com.onlinechat.dto.UserRegisterDTO;
import com.onlinechat.security.CurrentUser;
import com.onlinechat.service.AuthService;
import com.onlinechat.vo.LoginVO;
import com.onlinechat.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "认证接口", description = "登录、注册、当前用户")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册成功后返回JWT令牌，并自动创建默认好友分组")
    public Result<LoginVO> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT令牌")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户")
    public Result<UserVO> me() {
        return Result.success(authService.currentUser(currentUser.id()));
    }
}
