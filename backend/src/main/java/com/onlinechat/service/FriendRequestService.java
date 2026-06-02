package com.onlinechat.service;

import com.onlinechat.common.PageResult;
import com.onlinechat.dto.FriendRequestCreateDTO;
import com.onlinechat.dto.FriendRequestHandleDTO;
import com.onlinechat.vo.FriendRequestVO;

public interface FriendRequestService {

    void createRequest(Long senderId, FriendRequestCreateDTO dto);

    PageResult<FriendRequestVO> pageRequests(Long userId, String direction, String status, long pageNo, long pageSize);

    void accept(Long receiverId, Long requestId, FriendRequestHandleDTO dto);

    void reject(Long receiverId, Long requestId, FriendRequestHandleDTO dto);
}
