package com.onlinechat.service;

import com.onlinechat.common.PageResult;
import com.onlinechat.dto.FriendMoveDTO;
import com.onlinechat.dto.FriendRemarkDTO;
import com.onlinechat.vo.FriendDetailVO;
import com.onlinechat.vo.FriendVO;

public interface FriendService {

    PageResult<FriendVO> pageFriends(Long userId, Long groupId, String keyword, long pageNo, long pageSize);

    FriendDetailVO detail(Long userId, Long friendId);

    void updateRemark(Long userId, Long friendId, FriendRemarkDTO dto);

    void moveToGroup(Long userId, Long friendId, FriendMoveDTO dto);

    void deleteFriend(Long userId, Long friendId);

    boolean areFriends(Long userId, Long friendId);
}
