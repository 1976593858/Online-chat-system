package com.onlinechat.service;

import com.onlinechat.dto.FriendGroupCreateDTO;
import com.onlinechat.dto.FriendGroupUpdateDTO;
import com.onlinechat.entity.FriendGroup;
import com.onlinechat.vo.FriendGroupVO;

import java.util.List;

public interface FriendGroupService {

    FriendGroup getOrCreateDefaultGroup(Long userId);

    List<FriendGroupVO> listGroups(Long userId);

    FriendGroupVO createGroup(Long userId, FriendGroupCreateDTO dto);

    FriendGroupVO updateGroup(Long userId, Long groupId, FriendGroupUpdateDTO dto);

    void deleteGroup(Long userId, Long groupId);

    FriendGroup requireOwnedGroup(Long userId, Long groupId);
}
