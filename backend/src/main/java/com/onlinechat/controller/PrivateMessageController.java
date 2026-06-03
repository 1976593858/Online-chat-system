package com.onlinechat.controller;

import com.onlinechat.common.PageResult;
import com.onlinechat.common.Result;
import com.onlinechat.dto.PageQuery;
import com.onlinechat.dto.PrivateMessageSendDTO;
import com.onlinechat.security.CurrentUser;
import com.onlinechat.service.PrivateMessageService;
import com.onlinechat.vo.PrivateMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
@Tag(name = "私聊消息接口", description = "发送消息、查询聊天记录、导出聊天记录")
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;
    private final CurrentUser currentUser;

    @PostMapping("/private")
    @Operation(summary = "发送私聊消息")
    public Result<PrivateMessageVO> send(@Valid @RequestBody PrivateMessageSendDTO dto) {
        return Result.success(privateMessageService.send(currentUser.id(), dto));
    }

    @GetMapping("/private/{targetUserId}")
    @Operation(summary = "分页查询私聊聊天记录", description = "按时间倒序分页返回聊天记录")
    public Result<PageResult<PrivateMessageVO>> history(
            @Parameter(description = "目标用户ID") @PathVariable Long targetUserId,
            @Valid @ParameterObject PageQuery pageQuery) {
        return Result.success(privateMessageService.history(currentUser.id(), targetUserId, pageQuery.getPageNo(), pageQuery.getPageSize()));
    }

    @GetMapping("/private/{targetUserId}/export")
    @Operation(summary = "导出私聊聊天记录", description = "返回 txt 文件字节流")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "目标用户ID") @PathVariable Long targetUserId) {
        byte[] body = privateMessageService.exportText(currentUser.id(), targetUserId);
        String filename = URLEncoder.encode("chat-" + targetUserId + ".txt", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }
}

