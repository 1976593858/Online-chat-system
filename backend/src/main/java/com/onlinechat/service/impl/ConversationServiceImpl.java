package com.onlinechat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinechat.common.PageResult;
import com.onlinechat.common.ResultCode;
import com.onlinechat.entity.Conversation;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.mapper.ConversationMapper;
import com.onlinechat.service.ConversationService;
import com.onlinechat.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;

    @Override
    public PageResult<ConversationVO> recent(Long ownerId, long pageNo, long pageSize) {
        IPage<ConversationVO> page = conversationMapper.selectRecentPage(new Page<>(pageNo, pageSize), ownerId);
        return PageResult.of(page);
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
    }
}
