package com.chat.controller;

import com.alibaba.fastjson.JSONObject;
import com.chat.entity.GroupMessage;
import com.chat.mapper.GroupMessageMapper;
import com.chat.websocket.GroupChatWebSocket;
import com.onlinechat.common.Result;
import com.onlinechat.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group")
@Tag(name = "群聊接口", description = "群消息发送与在线成员查询")
public class GroupMessageController {

    private final CurrentUser currentUser;
    private final GroupMessageMapper groupMessageMapper;

    @PostMapping("/send")
    @Operation(summary = "发送群消息", description = "消息入库后通过 WebSocket 向群内所有在线成员广播")
    public Result<Void> send(@RequestBody SendRequest body) {
        Long userId = currentUser.id();
        String userIdStr = String.valueOf(userId);
        Long groupId = Long.valueOf(body.getGroupId());
        String content = body.getContent();

        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setFromUserId(userId);
        message.setContent(content);
        message.setMessageType("TEXT");
        message.setCreatedAt(LocalDateTime.now());
        groupMessageMapper.insert(message);

        JSONObject msg = new JSONObject();
        msg.put("type", "group_message");
        msg.put("messageId", message.getId());
        msg.put("groupId", body.getGroupId());
        msg.put("userId", userIdStr);
        msg.put("content", content);
        msg.put("timestamp", System.currentTimeMillis());

        GroupChatWebSocket.broadcast(body.getGroupId(), msg.toJSONString());
        return Result.success();
    }

    @GetMapping("/online/{groupId}")
    @Operation(summary = "查询群在线成员", description = "返回指定群组当前在线的用户 ID 列表")
    public Result<java.util.List<String>> online(
            @Parameter(description = "群ID") @PathVariable @NotBlank String groupId) {
        return Result.success(GroupChatWebSocket.getOnlineMembers(groupId));
    }

    @lombok.Data
    static class SendRequest {
        @NotBlank
        @io.swagger.v3.oas.annotations.media.Schema(description = "群ID")
        private String groupId;
        @NotBlank
        @io.swagger.v3.oas.annotations.media.Schema(description = "消息内容")
        private String content;
    }
}
