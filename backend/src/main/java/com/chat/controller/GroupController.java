package com.chat.controller;

import com.chat.dto.GroupCreateDTO;
import com.chat.dto.GroupUpdateDTO;
import com.chat.service.GroupService;
import com.chat.vo.GroupMemberVO;
import com.chat.vo.GroupMessageVO;
import com.chat.vo.GroupVO;
import com.onlinechat.common.PageResult;
import com.onlinechat.common.Result;
import com.onlinechat.dto.PageQuery;
import com.onlinechat.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
@Tag(name = "群聊管理接口", description = "创建群聊、修改群聊、加入/退出、群详情、群成员、群消息、踢人")
public class GroupController {

    private final GroupService groupService;
    private final CurrentUser currentUser;

    @PostMapping
    @Operation(summary = "创建群聊", description = "创建群聊，创建人自动成为群主")
    public Result<GroupVO> create(@Valid @RequestBody GroupCreateDTO dto) {
        return Result.success(groupService.createGroup(currentUser.id(), dto));
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "修改群聊信息", description = "群主或管理员可修改群名称和公告")
    public Result<GroupVO> update(
            @Parameter(description = "群ID") @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateDTO dto) {
        return Result.success(groupService.updateGroup(currentUser.id(), groupId, dto));
    }

    @GetMapping
    @Operation(summary = "查询我的群聊", description = "返回当前用户加入的所有群聊")
    public Result<List<GroupVO>> list() {
        return Result.success(groupService.listUserGroups(currentUser.id()));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "查询群详情")
    public Result<GroupVO> detail(@Parameter(description = "群ID") @PathVariable Long groupId) {
        return Result.success(groupService.getGroupDetail(groupId));
    }

    @PostMapping("/{groupId}/join")
    @Operation(summary = "加入群聊")
    public Result<Void> join(@Parameter(description = "群ID") @PathVariable Long groupId) {
        groupService.joinGroup(currentUser.id(), groupId);
        return Result.success();
    }

    @PostMapping("/{groupId}/leave")
    @Operation(summary = "退出群聊", description = "退出群聊；群主退出时自动转让给下一位成员，若群内无成员则自动解散")
    public Result<Void> leave(@Parameter(description = "群ID") @PathVariable Long groupId) {
        groupService.leaveGroup(currentUser.id(), groupId);
        return Result.success();
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "查询群成员列表", description = "需是群成员才能查看")
    public Result<List<GroupMemberVO>> members(@Parameter(description = "群ID") @PathVariable Long groupId) {
        return Result.success(groupService.getGroupMembers(groupId, currentUser.id()));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "移除群成员", description = "群主或管理员可移除成员；不能移除群主")
    public Result<Void> removeMember(
            @Parameter(description = "群ID") @PathVariable Long groupId,
            @Parameter(description = "待移除成员的用户ID") @PathVariable Long memberId) {
        groupService.removeMember(currentUser.id(), groupId, memberId);
        return Result.success();
    }

    @GetMapping("/{groupId}/messages")
    @Operation(summary = "分页查询群历史消息", description = "按时间倒序分页返回群聊天记录，需是群成员")
    public Result<PageResult<GroupMessageVO>> messages(
            @Parameter(description = "群ID") @PathVariable Long groupId,
            @Valid @ParameterObject PageQuery pageQuery) {
        return Result.success(groupService.getGroupMessages(groupId, currentUser.id(), pageQuery.getPageNo(), pageQuery.getPageSize()));
    }

    @GetMapping("/{groupId}/export")
    @Operation(summary = "导出群聊天记录", description = "返回 txt 文件字节流，需是群成员")
    public ResponseEntity<byte[]> export(@Parameter(description = "群ID") @PathVariable Long groupId) {
        byte[] body = groupService.exportGroupMessages(groupId, currentUser.id());
        String filename = URLEncoder.encode("group-chat-" + groupId + ".txt", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }
}
