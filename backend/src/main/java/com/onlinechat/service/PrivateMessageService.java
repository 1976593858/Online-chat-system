package com.onlinechat.service;

import com.onlinechat.common.PageResult;
import com.onlinechat.dto.PrivateMessageSendDTO;
import com.onlinechat.vo.PrivateMessageVO;

public interface PrivateMessageService {

    PrivateMessageVO send(Long fromUserId, PrivateMessageSendDTO dto);

    PageResult<PrivateMessageVO> history(Long userId, Long targetUserId, long pageNo, long pageSize);

    byte[] exportText(Long userId, Long targetUserId);
}

