package com.onlinechat.service;

import com.onlinechat.common.PageResult;
import com.onlinechat.vo.ConversationVO;

public interface ConversationService {

    PageResult<ConversationVO> recent(Long ownerId, long pageNo, long pageSize);

    ConversationVO openPrivate(Long ownerId, Long targetUserId);

    void markRead(Long ownerId, Long conversationId);
}
