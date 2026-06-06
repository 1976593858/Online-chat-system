package com.chat.controller;

import com.alibaba.fastjson.JSONObject;
import com.chat.dto.SendMessageDTO;
import com.chat.entity.GroupMember;
import com.chat.entity.GroupMessage;
import com.chat.mapper.GroupMemberMapper;
import com.chat.mapper.GroupMessageMapper;
import com.chat.websocket.GroupChatWebSocket;
import com.onlinechat.common.Result;
import com.onlinechat.common.ResultCode;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group")
@Tag(name = "群聊接口", description = "群消息发送与在线成员查询")
public class GroupMessageController {

    private final CurrentUser currentUser;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupMemberMapper groupMemberMapper;

    @PostMapping("/send")
    @Operation(summary = "发送群消息", description = "消息入库后通过 WebSocket 向群内所有在线成员广播")
    public Result<Void> send(@Valid @RequestBody SendMessageDTO body) {
        Long userId = currentUser.id();
        Long groupId = Long.valueOf(body.getGroupId());

        // Verify membership
        if (!groupMemberMapper.isMember(groupId, userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不是该群成员");
        }

        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setFromUserId(userId);
        message.setContent(body.getContent());
        message.setMessageType("TEXT");
        message.setCreatedAt(LocalDateTime.now());
        groupMessageMapper.insert(message);

        JSONObject msg = new JSONObject();
        msg.put("type", "group_message");
        msg.put("messageId", message.getId());
        msg.put("groupId", body.getGroupId());
        msg.put("userId", String.valueOf(userId));
        msg.put("content", body.getContent());
        msg.put("timestamp", System.currentTimeMillis());

        GroupChatWebSocket.broadcast(body.getGroupId(), msg.toJSONString());
        return Result.success();
    }

    @GetMapping("/online/{groupId}")
    @Operation(summary = "查询群在线成员", description = "返回指定群组当前在线的用户 ID 列表")
    public Result<List<String>> online(
            @Parameter(description = "群ID") @PathVariable String groupId) {
        return Result.success(GroupChatWebSocket.getOnlineMembers(groupId));
    }
}
