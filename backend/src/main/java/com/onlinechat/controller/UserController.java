package com.onlinechat.controller;

import com.onlinechat.common.PageResult;
import com.onlinechat.common.Result;
import com.onlinechat.dto.PageQuery;
import com.onlinechat.security.CurrentUser;
import com.onlinechat.service.UserService;
import com.onlinechat.vo.UserSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "用户接口", description = "用户搜索")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "按用户名、昵称、邮箱模糊搜索，并返回与当前用户的关系状态")
    public Result<PageResult<UserSearchVO>> search(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Valid @ParameterObject PageQuery pageQuery) {
        return Result.success(userService.search(currentUser.id(), keyword, pageQuery.getPageNo(), pageQuery.getPageSize()));
    }
}
