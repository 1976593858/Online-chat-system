package com.onlinechat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinechat.common.FriendshipStatus;
import com.onlinechat.common.PageResult;
import com.onlinechat.common.ResultCode;
import com.onlinechat.dto.FriendMoveDTO;
import com.onlinechat.dto.FriendRemarkDTO;
import com.onlinechat.entity.Friendship;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.mapper.FriendshipMapper;
import com.onlinechat.service.FriendGroupService;
import com.onlinechat.service.FriendService;
import com.onlinechat.vo.FriendDetailVO;
import com.onlinechat.vo.FriendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendshipMapper friendshipMapper;
    private final FriendGroupService friendGroupService;

    @Override
    public PageResult<FriendVO> pageFriends(Long userId, Long groupId, String keyword, long pageNo, long pageSize) {
        if (groupId != null) {
            friendGroupService.requireOwnedGroup(userId, groupId);
        }
        IPage<FriendVO> page = friendshipMapper.selectFriendPage(new Page<>(pageNo, pageSize), userId, groupId, keyword);
        return PageResult.of(page);
    }

    @Override
    public FriendDetailVO detail(Long userId, Long friendId) {
        FriendDetailVO detail = friendshipMapper.selectFriendDetail(userId, friendId);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "好友不存在");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRemark(Long userId, Long friendId, FriendRemarkDTO dto) {
        Friendship friendship = requireFriendship(userId, friendId);
        friendship.setRemark(dto.getRemark());
        friendship.setUpdatedAt(LocalDateTime.now());
        friendshipMapper.updateById(friendship);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveToGroup(Long userId, Long friendId, FriendMoveDTO dto) {
        friendGroupService.requireOwnedGroup(userId, dto.getGroupId());
        Friendship friendship = requireFriendship(userId, friendId);
        friendship.setGroupId(dto.getGroupId());
        friendship.setUpdatedAt(LocalDateTime.now());
        friendshipMapper.updateById(friendship);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long userId, Long friendId) {
        if (!areFriends(userId, friendId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "好友不存在");
        }
        friendshipMapper.delete(Wrappers.<Friendship>lambdaQuery()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getFriendId, friendId)
                .eq(Friendship::getStatus, FriendshipStatus.ACTIVE));
        friendshipMapper.delete(Wrappers.<Friendship>lambdaQuery()
                .eq(Friendship::getUserId, friendId)
                .eq(Friendship::getFriendId, userId)
                .eq(Friendship::getStatus, FriendshipStatus.ACTIVE));
    }

    @Override
    public boolean areFriends(Long userId, Long friendId) {
        return friendshipMapper.selectCount(Wrappers.<Friendship>lambdaQuery()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getFriendId, friendId)
                .eq(Friendship::getStatus, FriendshipStatus.ACTIVE)) > 0;
    }

    private Friendship requireFriendship(Long userId, Long friendId) {
        Friendship friendship = friendshipMapper.selectOne(Wrappers.<Friendship>lambdaQuery()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getFriendId, friendId)
                .eq(Friendship::getStatus, FriendshipStatus.ACTIVE)
                .last("LIMIT 1"));
        if (friendship == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "好友不存在");
        }
        return friendship;
    }
}
