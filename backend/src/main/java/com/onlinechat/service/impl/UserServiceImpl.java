package com.onlinechat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinechat.common.FriendshipStatus;
import com.onlinechat.common.PageResult;
import com.onlinechat.common.RelationStatus;
import com.onlinechat.common.RequestStatus;
import com.onlinechat.entity.FriendRequest;
import com.onlinechat.entity.Friendship;
import com.onlinechat.entity.User;
import com.onlinechat.mapper.FriendRequestMapper;
import com.onlinechat.mapper.FriendshipMapper;
import com.onlinechat.mapper.UserMapper;
import com.onlinechat.service.UserService;
import com.onlinechat.vo.UserSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;
    private final FriendRequestMapper friendRequestMapper;

    @Override
    public PageResult<UserSearchVO> search(Long currentUserId, String keyword, long pageNo, long pageSize) {
        IPage<User> page = userMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(User::getUsername, keyword.trim())
                                .or()
                                .like(User::getNickname, keyword.trim())
                                .or()
                                .like(User::getEmail, keyword.trim()))
                        .orderByDesc(User::getCreatedAt));

        List<UserSearchVO> records = page.getRecords().stream()
                .map(user -> toSearchVO(currentUserId, user))
                .toList();
        return PageResult.of(page, records);
    }

    private UserSearchVO toSearchVO(Long currentUserId, User user) {
        UserSearchVO vo = new UserSearchVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setRelationStatus(resolveRelationStatus(currentUserId, user.getId()));
        return vo;
    }

    private String resolveRelationStatus(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return RelationStatus.SELF;
        }
        if (friendshipMapper.selectCount(Wrappers.<Friendship>lambdaQuery()
                .eq(Friendship::getUserId, currentUserId)
                .eq(Friendship::getFriendId, targetUserId)
                .eq(Friendship::getStatus, FriendshipStatus.ACTIVE)) > 0) {
            return RelationStatus.FRIEND;
        }
        if (friendRequestMapper.selectCount(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getSenderId, currentUserId)
                .eq(FriendRequest::getReceiverId, targetUserId)
                .eq(FriendRequest::getStatus, RequestStatus.PENDING)) > 0) {
            return RelationStatus.PENDING_SENT;
        }
        if (friendRequestMapper.selectCount(Wrappers.<FriendRequest>lambdaQuery()
                .eq(FriendRequest::getSenderId, targetUserId)
                .eq(FriendRequest::getReceiverId, currentUserId)
                .eq(FriendRequest::getStatus, RequestStatus.PENDING)) > 0) {
            return RelationStatus.PENDING_RECEIVED;
        }
        return RelationStatus.NONE;
    }
}
