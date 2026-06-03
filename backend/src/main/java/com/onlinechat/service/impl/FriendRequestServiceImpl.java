package com.onlinechat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinechat.common.FriendshipStatus;
import com.onlinechat.common.PageResult;
import com.onlinechat.common.RequestStatus;
import com.onlinechat.common.ResultCode;
import com.onlinechat.dto.FriendRequestCreateDTO;
import com.onlinechat.dto.FriendRequestHandleDTO;
import com.onlinechat.entity.FriendGroup;
import com.onlinechat.entity.FriendRequest;
import com.onlinechat.entity.Friendship;
import com.onlinechat.entity.User;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.mapper.FriendRequestMapper;
import com.onlinechat.mapper.FriendshipMapper;
import com.onlinechat.mapper.UserMapper;
import com.onlinechat.service.FriendGroupService;
import com.onlinechat.service.FriendRequestService;
import com.onlinechat.service.FriendService;
import com.onlinechat.vo.FriendRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl implements FriendRequestService {

    private final FriendRequestMapper friendRequestMapper;
    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;
    private final FriendService friendService;
    private final FriendGroupService friendGroupService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRequest(Long senderId, FriendRequestCreateDTO dto) {
        if (senderId.equals(dto.getReceiverId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能添加自己为好友");
        }
        User receiver = userMapper.selectById(dto.getReceiverId());
        if (receiver == null || receiver.getStatus() == null || receiver.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标用户不存在");
        }
        if (friendService.areFriends(senderId, dto.getReceiverId())) {
            throw new BusinessException(ResultCode.CONFLICT, "已经是好友");
        }
        if (hasPendingRequest(senderId, dto.getReceiverId()) || hasPendingRequest(dto.getReceiverId(), senderId)) {
            throw new BusinessException(ResultCode.CONFLICT, "存在待处理的好友申请");
        }

        LocalDateTime now = LocalDateTime.now();
        FriendRequest request = new FriendRequest();
        request.setSenderId(senderId);
        request.setReceiverId(dto.getReceiverId());
        request.setMessage(StringUtils.hasText(dto.getMessage()) ? dto.getMessage().trim() : null);
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        friendRequestMapper.insert(request);
    }

    @Override
    public PageResult<FriendRequestVO> pageRequests(Long userId, String direction, String status, long pageNo, long pageSize) {
        String requestDirection = "sent".equalsIgnoreCase(direction) ? "sent" : "received";
        String requestStatus = StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
        IPage<FriendRequestVO> page = friendRequestMapper.selectRequestPage(
                new Page<>(pageNo, pageSize), userId, requestDirection, requestStatus);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long receiverId, Long requestId, FriendRequestHandleDTO dto) {
        FriendRequestHandleDTO handleDTO = dto == null ? new FriendRequestHandleDTO() : dto;
        FriendRequest request = requirePendingReceiverRequest(receiverId, requestId);
        if (friendService.areFriends(request.getSenderId(), request.getReceiverId())) {
            markHandled(request, RequestStatus.ACCEPTED, null);
            return;
        }

        FriendGroup receiverGroup = handleDTO.getGroupId() == null
                ? friendGroupService.getOrCreateDefaultGroup(receiverId)
                : friendGroupService.requireOwnedGroup(receiverId, handleDTO.getGroupId());
        FriendGroup senderGroup = friendGroupService.getOrCreateDefaultGroup(request.getSenderId());
        LocalDateTime now = LocalDateTime.now();
        friendshipMapper.insert(buildFriendship(receiverId, request.getSenderId(), receiverGroup.getId(), handleDTO.getRemark(), now));
        friendshipMapper.insert(buildFriendship(request.getSenderId(), receiverId, senderGroup.getId(), null, now));
        markHandled(request, RequestStatus.ACCEPTED, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long receiverId, Long requestId, FriendRequestHandleDTO dto) {
        FriendRequest request = requirePendingReceiverRequest(receiverId, requestId);
        String reason = dto == null || !StringUtils.hasText(dto.getHandleReason()) ? null : dto.getHandleReason().trim();
        markHandled(request, RequestStatus.REJECTED, reason);
    }

    private boolean hasPendingRequest(Long senderId, Long receiverId) {
        return friendRequestMapper.selectCount(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getSenderId, senderId)
                .eq(FriendRequest::getReceiverId, receiverId)
                .eq(FriendRequest::getStatus, RequestStatus.PENDING)) > 0;
    }

    private FriendRequest requirePendingReceiverRequest(Long receiverId, Long requestId) {
        FriendRequest request = friendRequestMapper.selectOne(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getId, requestId)
                .eq(FriendRequest::getReceiverId, receiverId)
                .eq(FriendRequest::getStatus, RequestStatus.PENDING)
                .last("LIMIT 1"));
        if (request == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "待处理好友申请不存在");
        }
        return request;
    }

    private Friendship buildFriendship(Long userId, Long friendId, Long groupId, String remark, LocalDateTime now) {
        Friendship friendship = new Friendship();
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendship.setGroupId(groupId);
        friendship.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        friendship.setStatus(FriendshipStatus.ACTIVE);
        friendship.setCreatedAt(now);
        friendship.setUpdatedAt(now);
        return friendship;
    }

    private void markHandled(FriendRequest request, String status, String reason) {
        request.setStatus(status);
        request.setHandleReason(reason);
        request.setHandledAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        friendRequestMapper.updateById(request);
    }
}
