package com.onlinechat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinechat.common.PageResult;
import com.onlinechat.common.ResultCode;
import com.onlinechat.entity.Conversation;
import com.onlinechat.entity.User;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.mapper.ConversationMapper;
import com.onlinechat.mapper.PrivateMessageMapper;
import com.onlinechat.mapper.UserMapper;
import com.onlinechat.service.ConversationService;
import com.onlinechat.service.FriendService;
import com.onlinechat.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final UserMapper userMapper;
    private final FriendService friendService;
    private final PrivateMessageMapper privateMessageMapper;

    @Override
    public PageResult<ConversationVO> recent(Long ownerId, long pageNo, long pageSize) {
        IPage<ConversationVO> page = conversationMapper.selectRecentPage(new Page<>(pageNo, pageSize), ownerId);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationVO openPrivate(Long ownerId, Long targetUserId) {
        if (ownerId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能与自己私聊");
        }
        requireActiveUser(targetUserId);
        if (!friendService.areFriends(ownerId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能与好友私聊");
        }

        LocalDateTime now = LocalDateTime.now();
        ensureConversation(ownerId, targetUserId, now);
        ensureConversation(targetUserId, ownerId, now);

        ConversationVO vo = conversationMapper.selectPrivateOne(ownerId, targetUserId);
        if (vo == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "会话创建失败");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long ownerId, Long conversationId) {
        Conversation conversation = conversationMapper.selectOne(Wrappers.<Conversation>lambdaQuery()
                .eq(Conversation::getId, conversationId)
                .eq(Conversation::getOwnerId, ownerId)
                .last("LIMIT 1"));
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        conversation.setUnreadCount(0);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        if ("PRIVATE".equalsIgnoreCase(conversation.getConversationType()) && conversation.getTargetUserId() != null) {
            privateMessageMapper.markRead(ownerId, conversation.getTargetUserId(), LocalDateTime.now());
        }
    }

    private void ensureConversation(Long ownerId, Long targetUserId, LocalDateTime now) {
        Conversation conversation = conversationMapper.selectOne(Wrappers.<Conversation>lambdaQuery()
                .eq(Conversation::getOwnerId, ownerId)
                .eq(Conversation::getTargetUserId, targetUserId)
                .eq(Conversation::getConversationType, "PRIVATE")
                .last("LIMIT 1"));

        if (conversation != null) {
            return;
        }

        Conversation create = new Conversation();
        create.setOwnerId(ownerId);
        create.setTargetUserId(targetUserId);
        create.setConversationType("PRIVATE");
        create.setUnreadCount(0);
        create.setPinned(0);
        create.setMuted(0);
        create.setLastMessageAt(now);
        create.setCreatedAt(now);
        create.setUpdatedAt(now);
        conversationMapper.insert(create);
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标用户不存在");
        }
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标用户不存在");
        }
        return user;
    }
}
