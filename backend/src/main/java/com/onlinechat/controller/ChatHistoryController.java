package com.onlinechat.controller;

import com.onlinechat.common.PageResult;
import com.onlinechat.common.Result;
import com.onlinechat.security.CurrentUser;
import com.onlinechat.service.ChatHistoryService;
import com.onlinechat.vo.ChatHistorySearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-history")
@Tag(name = "聊天记录查询", description = "基于 MySQL 全文检索的聊天记录搜索")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;
    private final CurrentUser currentUser;

    @GetMapping("/search")
    @Operation(summary = "搜索聊天记录", description = "全文检索私聊消息，支持分页、按用户筛选、日期范围筛选")
    public Result<PageResult<ChatHistorySearchVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户ID（筛选与该用户的聊天）") @RequestParam(required = false) Long userId,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String fromDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String toDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNo,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(chatHistoryService.search(currentUser.id(), keyword, userId, fromDate, toDate, pageNo, pageSize));
    }
}
