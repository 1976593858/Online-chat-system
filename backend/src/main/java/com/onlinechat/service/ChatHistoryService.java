package com.onlinechat.service;

import com.onlinechat.common.PageResult;
import com.onlinechat.vo.ChatHistorySearchVO;

public interface ChatHistoryService {

    PageResult<ChatHistorySearchVO> search(Long currentUserId, String keyword, Long userId, String fromDate, String toDate, long pageNo, long pageSize);
}
