package com.onlinechat.controller;

import com.onlinechat.common.PageResult;
import com.onlinechat.common.Result;
import com.onlinechat.dto.PageQuery;
import com.onlinechat.security.CurrentUser;
import com.onlinechat.service.ConversationService;
import com.onlinechat.vo.ConversationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
@Tag(name = "消息列表接口", description = "最近会话、最后消息、未读数量")
public class ConversationController {

    private final ConversationService conversationService;
    private final CurrentUser currentUser;

    @GetMapping("/recent")
    @Operation(summary = "获取最近会话列表", description = "按置顶和最后消息时间倒序排序，包含最后一条消息和未读数量")
    public Result<PageResult<ConversationVO>> recent(@Valid @ParameterObject PageQuery pageQuery) {
        return Result.success(conversationService.recent(currentUser.id(), pageQuery.getPageNo(), pageQuery.getPageSize()));
    }

    @PutMapping("/{conversationId}/read")
    @Operation(summary = "标记会话已读", description = "将当前用户指定会话的未读数清零")
    public Result<Void> markRead(@Parameter(description = "会话ID") @PathVariable Long conversationId) {
        conversationService.markRead(currentUser.id(), conversationId);
        return Result.success();
    }
}
