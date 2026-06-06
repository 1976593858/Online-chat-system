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

    /** Preview group info without requiring membership */
    GroupVO getGroupPreview(Long groupId);

    /** Join by numeric group ID */
    void joinGroup(Long userId, Long groupId);

    /** Join by invite code */
    GroupVO joinByInviteCode(Long userId, String code);

    void leaveGroup(Long userId, Long groupId);

    void removeMember(Long operatorId, Long groupId, Long memberId);

    List<GroupMemberVO> getGroupMembers(Long groupId, Long userId);

    PageResult<GroupMessageVO> getGroupMessages(Long groupId, Long userId, long pageNo, long pageSize);

    byte[] exportGroupMessages(Long groupId, Long userId);

    /** Invite a user to the group (must be a member to invite) */
    void inviteMember(Long senderId, Long groupId, Long inviteeId);

    /** Toggle mute for current user in a group */
    void toggleMute(Long userId, Long groupId, boolean muted);

    /** Get pending invites for current user */
    List<com.chat.vo.GroupInviteVO> getPendingInvites(Long userId);

    /** Accept a group invite */
    void acceptInvite(Long userId, Long inviteId);

    /** Reject a group invite */
    void rejectInvite(Long userId, Long inviteId);
}
