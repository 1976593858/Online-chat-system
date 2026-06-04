package com.onlinechat.controller;

import com.onlinechat.common.PageResult;
import com.onlinechat.common.Result;
import com.onlinechat.dto.FriendRequestCreateDTO;
import com.onlinechat.dto.FriendRequestHandleDTO;
import com.onlinechat.dto.PageQuery;
import com.onlinechat.security.CurrentUser;
import com.onlinechat.service.FriendRequestService;
import com.onlinechat.vo.FriendRequestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friend-requests")
@Tag(name = "好友申请接口", description = "发送、查询、同意、拒绝好友申请")
public class FriendRequestController {

    private final FriendRequestService friendRequestService;
    private final CurrentUser currentUser;

    @PostMapping
    @Operation(summary = "发送好友申请", description = "添加好友入口：发送待处理好友申请")
    public Result<Void> createRequest(@Valid @RequestBody FriendRequestCreateDTO dto) {
        friendRequestService.createRequest(currentUser.id(), dto);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "分页查询好友申请", description = "direction=received 查询收到的申请；direction=sent 查询发出的申请")
    public Result<PageResult<FriendRequestVO>> pageRequests(
            @Parameter(description = "方向：received 或 sent") @RequestParam(defaultValue = "received") String direction,
            @Parameter(description = "状态：PENDING、ACCEPTED、REJECTED") @RequestParam(required = false) String status,
            @Valid @ParameterObject PageQuery pageQuery) {
        return Result.success(friendRequestService.pageRequests(currentUser.id(), direction, status,
                pageQuery.getPageNo(), pageQuery.getPageSize()));
    }

    @PutMapping("/{requestId}/accept")
    @Operation(summary = "同意好友申请", description = "同意后双向建立好友关系；当前用户侧可指定分组和备注")
    public Result<Void> accept(
            @Parameter(description = "好友申请ID") @PathVariable Long requestId,
            @Valid @RequestBody(required = false) FriendRequestHandleDTO dto) {
        friendRequestService.accept(currentUser.id(), requestId, dto);
        return Result.success();
    }

    @PutMapping("/{requestId}/reject")
    @Operation(summary = "拒绝好友申请")
    public Result<Void> reject(
            @Parameter(description = "好友申请ID") @PathVariable Long requestId,
            @Valid @RequestBody(required = false) FriendRequestHandleDTO dto) {
        friendRequestService.reject(currentUser.id(), requestId, dto);
        return Result.success();
    }

    @PostMapping("/{requestId}/resend")
    @Operation(summary = "重新发送好友申请", description = "已拒绝的申请可重新发送，更新申请时间和备注")
    public Result<Void> resend(
            @Parameter(description = "好友申请ID") @PathVariable Long requestId,
            @RequestBody(required = false) FriendRequestCreateDTO dto) {
        friendRequestService.resend(currentUser.id(), requestId, dto == null ? new FriendRequestCreateDTO() : dto);
        return Result.success();
    }
}
