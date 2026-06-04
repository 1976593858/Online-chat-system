package com.onlinechat.service;

import com.onlinechat.common.PageResult;
import com.onlinechat.entity.PrivateMessage;
import com.onlinechat.vo.ChatHistorySearchVO;

public interface ChatHistoryService {

    void syncMessage(PrivateMessage message);

    PageResult<ChatHistorySearchVO> search(Long currentUserId, String keyword, Long userId, String fromDate, String toDate, long pageNo, long pageSize);

    long syncExistingMessages();
}
