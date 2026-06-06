package com.chat.service;

import com.chat.dto.GroupCreateDTO;
import com.chat.dto.GroupUpdateDTO;
import com.chat.vo.GroupMemberVO;
import com.chat.vo.GroupMessageVO;
import com.chat.vo.GroupVO;
import com.onlinechat.common.PageResult;

import java.util.List;

public interface GroupService {

    GroupVO createGroup(Long ownerId, GroupCreateDTO dto);

    GroupVO updateGroup(Long userId, Long groupId, GroupUpdateDTO dto);

    List<GroupVO> listUserGroups(Long userId);

    GroupVO getGroupDetail(Long groupId);

    void joinGroup(Long userId, Long groupId);

    void leaveGroup(Long userId, Long groupId);

    void removeMember(Long operatorId, Long groupId, Long memberId);

    List<GroupMemberVO> getGroupMembers(Long groupId, Long userId);

    PageResult<GroupMessageVO> getGroupMessages(Long groupId, Long userId, long pageNo, long pageSize);

    byte[] exportGroupMessages(Long groupId, Long userId);
}
