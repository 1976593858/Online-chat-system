package com.chat.service;

import com.chat.dto.GroupCreateDTO;
import com.chat.vo.GroupMemberVO;
import com.chat.vo.GroupMessageVO;
import com.chat.vo.GroupVO;
import com.onlinechat.common.PageResult;

import java.util.List;

public interface GroupService {

    GroupVO createGroup(Long ownerId, GroupCreateDTO dto);

    List<GroupVO> listUserGroups(Long userId);

    GroupVO getGroupDetail(Long groupId);

    void joinGroup(Long userId, Long groupId);

    void leaveGroup(Long userId, Long groupId);

    List<GroupMemberVO> getGroupMembers(Long groupId);

    PageResult<GroupMessageVO> getGroupMessages(Long groupId, long pageNo, long pageSize);

    byte[] exportGroupMessages(Long groupId);
}
