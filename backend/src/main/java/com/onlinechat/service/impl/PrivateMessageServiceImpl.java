package com.onlinechat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinechat.common.PageResult;
import com.onlinechat.common.ResultCode;
import com.onlinechat.dto.PrivateMessageSendDTO;
import com.onlinechat.entity.Conversation;
import com.onlinechat.entity.PrivateMessage;
import com.onlinechat.entity.User;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.mapper.ConversationMapper;
import com.onlinechat.mapper.PrivateMessageMapper;
import com.onlinechat.mapper.UserMapper;
import com.onlinechat.service.FriendService;
import com.onlinechat.service.PrivateMessageService;
import com.onlinechat.vo.PrivateMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl implements PrivateMessageService {

    private static final DateTimeFormatter EXPORT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PrivateMessageMapper privateMessageMapper;
    private final ConversationMapper conversationMapper;
    private final UserMapper userMapper;
    private final FriendService friendService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivateMessageVO send(Long fromUserId, PrivateMessageSendDTO dto) {
        if (fromUserId.equals(dto.getToUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能给自己发送消息");
        }
        requireActiveUser(dto.getToUserId());
        if (!friendService.areFriends(fromUserId, dto.getToUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能与好友私聊");
        }

        LocalDateTime now = LocalDateTime.now();
        PrivateMessage message = new PrivateMessage();
        message.setFromUserId(fromUserId);
        message.setToUserId(dto.getToUserId());
        message.setContent(dto.getContent() == null ? null : dto.getContent().trim());
        message.setMessageType(normalizeMessageType(dto.getMessageType()));
        message.setCreatedAt(now);
        privateMessageMapper.insert(message);

        upsertConversation(fromUserId, dto.getToUserId(), message, now, 0);
        upsertConversation(dto.getToUserId(), fromUserId, message, now, 1);

        return toVO(message);
    }

    @Override
    public PageResult<PrivateMessageVO> history(Long userId, Long targetUserId, long pageNo, long pageSize) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "参数错误");
        }
        requireActiveUser(targetUserId);
        if (!friendService.areFriends(userId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看与好友的私聊记录");
        }

        IPage<PrivateMessage> page = privateMessageMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<PrivateMessage>lambdaQuery()
                        .and(wrapper -> wrapper
                                .eq(PrivateMessage::getFromUserId, userId)
                                .eq(PrivateMessage::getToUserId, targetUserId)
                                .or()
                                .eq(PrivateMessage::getFromUserId, targetUserId)
                                .eq(PrivateMessage::getToUserId, userId))
                        .orderByDesc(PrivateMessage::getCreatedAt)
                        .orderByDesc(PrivateMessage::getId));

        List<PrivateMessageVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(page, records);
    }

    @Override
    public byte[] exportText(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "参数错误");
        }
        User current = requireActiveUser(userId);
        User target = requireActiveUser(targetUserId);
        if (!friendService.areFriends(userId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能导出与好友的私聊记录");
        }

        List<PrivateMessage> messages = privateMessageMapper.selectList(Wrappers.<PrivateMessage>lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(PrivateMessage::getFromUserId, userId)
                        .eq(PrivateMessage::getToUserId, targetUserId)
                        .or()
                        .eq(PrivateMessage::getFromUserId, targetUserId)
                        .eq(PrivateMessage::getToUserId, userId))
                .orderByAsc(PrivateMessage::getCreatedAt)
                .orderByAsc(PrivateMessage::getId));

        String currentName = displayName(current);
        String targetName = displayName(target);

        StringBuilder builder = new StringBuilder();
        for (PrivateMessage message : messages) {
            String sender = message.getFromUserId().equals(userId) ? currentName : targetName;
            builder.append(EXPORT_FORMATTER.format(message.getCreatedAt()))
                    .append(" ")
                    .append(sender)
                    .append(": ")
                    .append(message.getContent())
                    .append(System.lineSeparator());
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void upsertConversation(Long ownerId, Long targetUserId, PrivateMessage message, LocalDateTime now, int unreadIncrease) {
        Conversation conversation = conversationMapper.selectOne(Wrappers.<Conversation>lambdaQuery()
                .eq(Conversation::getOwnerId, ownerId)
                .eq(Conversation::getTargetUserId, targetUserId)
                .eq(Conversation::getConversationType, "PRIVATE")
                .last("LIMIT 1"));

        if (conversation == null) {
            conversation = new Conversation();
            conversation.setOwnerId(ownerId);
            conversation.setTargetUserId(targetUserId);
            conversation.setConversationType("PRIVATE");
            conversation.setUnreadCount(unreadIncrease);
            conversation.setPinned(0);
            conversation.setMuted(0);
            conversation.setCreatedAt(now);
            conversation.setUpdatedAt(now);
            conversation.setLastMessageAt(now);
            conversation.setLastMessageId(message.getId());
            conversation.setLastMessageContent(message.getContent());
            conversation.setLastMessageType(message.getMessageType());
            conversationMapper.insert(conversation);
            return;
        }

        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageContent(message.getContent());
        conversation.setLastMessageType(message.getMessageType());
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        if (unreadIncrease > 0) {
            conversation.setUnreadCount((conversation.getUnreadCount() == null ? 0 : conversation.getUnreadCount()) + unreadIncrease);
        }
        conversationMapper.updateById(conversation);
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标用户不存在");
        }
        return user;
    }

    private String normalizeMessageType(String messageType) {
        if (!StringUtils.hasText(messageType)) {
            return "TEXT";
        }
        return messageType.trim().toUpperCase(Locale.ROOT);
    }

    private PrivateMessageVO toVO(PrivateMessage message) {
        PrivateMessageVO vo = new PrivateMessageVO();
        vo.setId(message.getId());
        vo.setFromUserId(message.getFromUserId());
        vo.setToUserId(message.getToUserId());
        vo.setContent(message.getContent());
        vo.setMessageType(message.getMessageType());
        vo.setReadAt(message.getReadAt());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }
}

