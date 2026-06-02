package com.onlinechat.controller;

import com.onlinechat.common.Result;
import com.onlinechat.dto.FriendGroupCreateDTO;
import com.onlinechat.dto.FriendGroupUpdateDTO;
import com.onlinechat.security.CurrentUser;
import com.onlinechat.service.FriendGroupService;
import com.onlinechat.vo.FriendGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friend-groups")
@Tag(name = "好友分组接口", description = "默认分组、创建、修改、删除好友分组")
public class FriendGroupController {

    private final FriendGroupService friendGroupService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "获取好友分组列表", description = "包含默认分组和每个分组下的好友数量")
    public Result<List<FriendGroupVO>> listGroups() {
        return Result.success(friendGroupService.listGroups(currentUser.id()));
    }

    @PostMapping
    @Operation(summary = "创建好友分组")
    public Result<FriendGroupVO> createGroup(@Valid @RequestBody FriendGroupCreateDTO dto) {
        return Result.success(friendGroupService.createGroup(currentUser.id(), dto));
    }

    @PutMapping("/{groupId}")
    @Operation(summary = "修改好友分组名称")
    public Result<FriendGroupVO> updateGroup(
            @Parameter(description = "分组ID") @PathVariable Long groupId,
            @Valid @RequestBody FriendGroupUpdateDTO dto) {
        return Result.success(friendGroupService.updateGroup(currentUser.id(), groupId, dto));
    }

    @DeleteMapping("/{groupId}")
    @Operation(summary = "删除好友分组", description = "默认分组不可删除；删除普通分组时好友会移动到默认分组")
    public Result<Void> deleteGroup(@Parameter(description = "分组ID") @PathVariable Long groupId) {
        friendGroupService.deleteGroup(currentUser.id(), groupId);
        return Result.success();
    }
}
