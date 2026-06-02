package com.onlinechat.controller;

import com.onlinechat.common.PageResult;
import com.onlinechat.common.Result;
import com.onlinechat.dto.FriendMoveDTO;
import com.onlinechat.dto.FriendRemarkDTO;
import com.onlinechat.dto.PageQuery;
import com.onlinechat.security.CurrentUser;
import com.onlinechat.service.FriendService;
import com.onlinechat.vo.FriendDetailVO;
import com.onlinechat.vo.FriendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
@Tag(name = "好友管理接口", description = "好友列表、好友详情、备注、移动分组、删除好友")
public class FriendController {

    private final FriendService friendService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "分页查询好友列表", description = "支持按分组和关键字筛选")
    public Result<PageResult<FriendVO>> pageFriends(
            @Parameter(description = "好友分组ID") @RequestParam(required = false) Long groupId,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Valid @ParameterObject PageQuery pageQuery) {
        return Result.success(friendService.pageFriends(currentUser.id(), groupId, keyword,
                pageQuery.getPageNo(), pageQuery.getPageSize()));
    }

    @GetMapping("/{friendId}")
    @Operation(summary = "获取好友详情")
    public Result<FriendDetailVO> detail(@Parameter(description = "好友用户ID") @PathVariable Long friendId) {
        return Result.success(friendService.detail(currentUser.id(), friendId));
    }

    @PutMapping("/{friendId}/remark")
    @Operation(summary = "修改好友备注")
    public Result<Void> updateRemark(
            @Parameter(description = "好友用户ID") @PathVariable Long friendId,
            @Valid @RequestBody FriendRemarkDTO dto) {
        friendService.updateRemark(currentUser.id(), friendId, dto);
        return Result.success();
    }

    @PutMapping("/{friendId}/group")
    @Operation(summary = "移动好友到指定分组")
    public Result<Void> moveToGroup(
            @Parameter(description = "好友用户ID") @PathVariable Long friendId,
            @Valid @RequestBody FriendMoveDTO dto) {
        friendService.moveToGroup(currentUser.id(), friendId, dto);
        return Result.success();
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "删除好友", description = "双向删除好友关系")
    public Result<Void> deleteFriend(@Parameter(description = "好友用户ID") @PathVariable Long friendId) {
        friendService.deleteFriend(currentUser.id(), friendId);
        return Result.success();
    }
}
