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
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
@Tag(name = "消息列表接口", description = "最近会话、最后消息、未读数量、免打扰")
public class ConversationController {

    private final ConversationService conversationService;
    private final CurrentUser currentUser;

    @GetMapping("/recent")
    @Operation(summary = "获取最近会话列表", description = "按置顶和最后消息时间倒序排序，包含最后一条消息和未读数量")
    public Result<PageResult<ConversationVO>> recent(@Valid @ParameterObject PageQuery pageQuery) {
        return Result.success(conversationService.recent(currentUser.id(), pageQuery.getPageNo(), pageQuery.getPageSize()));
    }

    @GetMapping("/private/{targetUserId}")
    @Operation(summary = "打开或创建私聊会话", description = "用于进入与指定用户的私聊页面，返回当前用户侧会话信息")
    public Result<ConversationVO> openPrivate(@Parameter(description = "目标用户ID") @PathVariable Long targetUserId) {
        return Result.success(conversationService.openPrivate(currentUser.id(), targetUserId));
    }

    @PutMapping("/{conversationId}/read")
    @Operation(summary = "标记会话已读", description = "将当前用户指定会话的未读数清零")
    public Result<Void> markRead(@Parameter(description = "会话ID") @PathVariable Long conversationId) {
        conversationService.markRead(currentUser.id(), conversationId);
        return Result.success();
    }

    @PutMapping("/{conversationId}/mute")
    @Operation(summary = "设置会话免打扰", description = "开启后该会话的新消息不会增加未读数和推送通知")
    public Result<Void> toggleMute(
            @Parameter(description = "会话ID") @PathVariable Long conversationId,
            @RequestParam boolean muted) {
        conversationService.toggleMute(currentUser.id(), conversationId, muted);
        return Result.success();
    }
}
