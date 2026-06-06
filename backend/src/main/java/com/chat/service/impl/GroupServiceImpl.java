package com.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chat.dto.GroupCreateDTO;
import com.chat.dto.GroupUpdateDTO;
import com.chat.entity.GroupInfo;
import com.chat.entity.GroupInvite;
import com.chat.entity.GroupMember;
import com.chat.entity.GroupMessage;
import com.chat.mapper.GroupInfoMapper;
import com.chat.mapper.GroupInviteMapper;
import com.chat.mapper.GroupMemberMapper;
import com.chat.mapper.GroupMessageMapper;
import com.chat.service.GroupService;
import com.onlinechat.mapper.UserMapper;
import com.chat.vo.GroupInviteVO;
import com.chat.vo.GroupMemberVO;
import com.chat.vo.GroupMessageVO;
import com.chat.vo.GroupVO;
import com.chat.websocket.GroupChatWebSocket;
import com.onlinechat.common.PageResult;
import com.onlinechat.common.ResultCode;
import com.onlinechat.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private static final DateTimeFormatter EXPORT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GroupInfoMapper groupInfoMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final GroupInviteMapper groupInviteMapper;
    private final UserMapper userMapper;

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO createGroup(Long ownerId, GroupCreateDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        GroupInfo group = new GroupInfo();
        group.setName(dto.getName().trim());
        group.setAnnouncement(dto.getAnnouncement());
        group.setOwnerId(ownerId);
        group.setInviteCode(generateUniqueInviteCode());
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        groupInfoMapper.insert(group);

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(ownerId);
        member.setRole("OWNER");
        member.setMuted(0);
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        groupMemberMapper.insert(member);

        return groupInfoMapper.selectGroupDetail(group.getId());
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < 10; i++) {
            String code = generateInviteCode();
            Long count = groupInfoMapper.selectCount(Wrappers.<GroupInfo>lambdaQuery()
                    .eq(GroupInfo::getInviteCode, code));
            if (count == null || count == 0) return code;
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "生成邀请码失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO updateGroup(Long userId, Long groupId, GroupUpdateDTO dto) {
        GroupInfo group = ensureGroupExists(groupId);
        GroupMember member = ensureMember(groupId, userId);
        if (!"OWNER".equals(member.getRole()) && !"ADMIN".equals(member.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅群主或管理员可以修改群信息");
        }
        if (StringUtils.hasText(dto.getName())) {
            group.setName(dto.getName().trim());
        }
        if (dto.getAnnouncement() != null) {
            group.setAnnouncement(dto.getAnnouncement());
        }
        group.setUpdatedAt(LocalDateTime.now());
        groupInfoMapper.updateById(group);
        return groupInfoMapper.selectGroupDetail(groupId);
    }

    @Override
    public List<GroupVO> listUserGroups(Long userId) {
        return groupInfoMapper.selectUserGroups(userId);
    }

    @Override
    public GroupVO getGroupDetail(Long groupId) {
        GroupVO vo = groupInfoMapper.selectGroupDetail(groupId);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群聊不存在");
        }
        return vo;
    }

    @Override
    public GroupVO getGroupPreview(Long groupId) {
        GroupInfo group = ensureGroupExists(groupId);
        return groupInfoMapper.selectGroupDetail(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinGroup(Long userId, Long groupId) {
        GroupInfo group = ensureGroupExists(groupId);
        if (groupMemberMapper.isMember(groupId, userId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole("MEMBER");
        member.setMuted(0);
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        groupMemberMapper.insert(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO joinByInviteCode(Long userId, String code) {
        GroupInfo group = groupInfoMapper.selectOne(Wrappers.<GroupInfo>lambdaQuery()
                .eq(GroupInfo::getInviteCode, code.trim().toUpperCase())
                .last("LIMIT 1"));
        if (group == null || (group.getDeleted() != null && group.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "邀请码无效或群聊不存在");
        }
        joinGroup(userId, group.getId());
        return groupInfoMapper.selectGroupDetail(group.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long userId, Long groupId) {
        GroupMember member = ensureMember(groupId, userId);
        if ("OWNER".equals(member.getRole())) {
            List<GroupMemberVO> members = groupMemberMapper.selectMembersByGroupId(groupId);
            GroupMemberVO successor = members.stream()
                    .filter(m -> !m.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
            if (successor == null) {
                groupInfoMapper.deleteById(groupId);
                groupMemberMapper.deleteById(member);
                return;
            }
            GroupMember successorMember = groupMemberMapper.selectOne(Wrappers.<GroupMember>lambdaQuery()
                    .eq(GroupMember::getGroupId, groupId)
                    .eq(GroupMember::getUserId, successor.getUserId())
                    .last("LIMIT 1"));
            successorMember.setRole("OWNER");
            groupMemberMapper.updateById(successorMember);
            GroupInfo group = groupInfoMapper.selectById(groupId);
            group.setOwnerId(successor.getUserId());
            groupInfoMapper.updateById(group);
        }
        groupMemberMapper.deleteById(member);

        long remainingCount = groupMemberMapper.selectCount(Wrappers.<GroupMember>lambdaQuery()
                .eq(GroupMember::getGroupId, groupId));
        if (remainingCount == 0) {
            groupInfoMapper.deleteById(groupId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long operatorId, Long groupId, Long memberId) {
        GroupMember operatorMember = ensureMember(groupId, operatorId);
        if (!"OWNER".equals(operatorMember.getRole()) && !"ADMIN".equals(operatorMember.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅群主或管理员可以移除成员");
        }
        GroupMember targetMember = ensureMember(groupId, memberId);
        if ("OWNER".equals(targetMember.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能移除群主");
        }
        if ("ADMIN".equals(targetMember.getRole()) && !"OWNER".equals(operatorMember.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "管理员不能移除其他管理员");
        }
        groupMemberMapper.deleteById(targetMember);
    }

    @Override
    public List<GroupMemberVO> getGroupMembers(Long groupId, Long userId) {
        ensureMember(groupId, userId);
        return groupMemberMapper.selectMembersByGroupId(groupId);
    }

    @Override
    public PageResult<GroupMessageVO> getGroupMessages(Long groupId, Long userId, long pageNo, long pageSize) {
        ensureMember(groupId, userId);
        IPage<GroupMessageVO> page = groupMessageMapper.selectPageByGroupId(
                new Page<>(pageNo, pageSize), groupId);
        return PageResult.of(page);
    }

    @Override
    public byte[] exportGroupMessages(Long groupId, Long userId) {
        ensureMember(groupId, userId);
        GroupVO group = groupInfoMapper.selectGroupDetail(groupId);
        List<GroupMessageVO> messages = groupMessageMapper.selectAllByGroupId(groupId);

        StringBuilder builder = new StringBuilder();
        builder.append("群聊：").append(group.getName()).append(System.lineSeparator());
        builder.append("导出时间：").append(EXPORT_FORMATTER.format(LocalDateTime.now()))
                .append(System.lineSeparator()).append(System.lineSeparator());

        for (GroupMessageVO msg : messages) {
            String senderName = msg.getFromNickname() != null ? msg.getFromNickname() : msg.getFromUsername();
            builder.append(EXPORT_FORMATTER.format(msg.getCreatedAt()))
                    .append(" ")
                    .append(senderName)
                    .append(": ")
                    .append(msg.getContent())
                    .append(System.lineSeparator());
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMember(Long senderId, Long groupId, Long inviteeId) {
        ensureMember(groupId, senderId);
        GroupInfo group = ensureGroupExists(groupId);

        if (groupMemberMapper.isMember(groupId, inviteeId)) {
            throw new BusinessException(ResultCode.CONFLICT, "该用户已在群中");
        }

        // Check for existing pending invite
        GroupInvite existing = groupInviteMapper.selectOne(Wrappers.<GroupInvite>lambdaQuery()
                .eq(GroupInvite::getGroupId, groupId)
                .eq(GroupInvite::getInviteeId, inviteeId)
                .eq(GroupInvite::getStatus, "PENDING")
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ResultCode.CONFLICT, "已发送过邀请，等待对方处理");
        }

        LocalDateTime now = LocalDateTime.now();
        GroupInvite invite = new GroupInvite();
        invite.setGroupId(groupId);
        invite.setSenderId(senderId);
        invite.setInviteeId(inviteeId);
        invite.setStatus("PENDING");
        invite.setCreatedAt(now);
        invite.setUpdatedAt(now);
        groupInviteMapper.insert(invite);

        // Notify invitee via WebSocket
        com.alibaba.fastjson.JSONObject msg = new com.alibaba.fastjson.JSONObject();
        msg.put("type", "group_invite");
        msg.put("action", "invited");
        msg.put("inviteId", invite.getId());
        msg.put("groupId", groupId);
        msg.put("groupName", group.getName());
        msg.put("fromUserId", senderId);
        GroupChatWebSocket.sendToUser(String.valueOf(inviteeId), msg.toJSONString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleMute(Long userId, Long groupId, boolean muted) {
        GroupMember member = ensureMember(groupId, userId);
        member.setMuted(muted ? 1 : 0);
        member.setUpdatedAt(LocalDateTime.now());
        groupMemberMapper.updateById(member);
    }

    @Override
    public List<GroupInviteVO> getPendingInvites(Long userId) {
        List<GroupInvite> invites = groupInviteMapper.selectList(Wrappers.<GroupInvite>lambdaQuery()
                .eq(GroupInvite::getInviteeId, userId)
                .eq(GroupInvite::getStatus, "PENDING")
                .orderByDesc(GroupInvite::getCreatedAt));

        return invites.stream().map(inv -> {
            GroupInviteVO vo = new GroupInviteVO();
            vo.setId(inv.getId());
            vo.setGroupId(inv.getGroupId());
            vo.setSenderId(inv.getSenderId());
            vo.setStatus(inv.getStatus());
            vo.setCreatedAt(inv.getCreatedAt());

            GroupVO g = groupInfoMapper.selectGroupDetail(inv.getGroupId());
            if (g != null) {
                vo.setGroupName(g.getName());
            }
            // Look up sender info
            if (inv.getSenderId() != null) {
                com.onlinechat.entity.User sender = userMapper.selectById(inv.getSenderId());
                if (sender != null) {
                    vo.setSenderUsername(sender.getUsername());
                    vo.setSenderNickname(sender.getNickname());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvite(Long userId, Long inviteId) {
        GroupInvite invite = groupInviteMapper.selectById(inviteId);
        if (invite == null || invite.getDeleted() != null && invite.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "邀请不存在");
        }
        if (!userId.equals(invite.getInviteeId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权处理该邀请");
        }
        if (!"PENDING".equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "邀请已处理");
        }

        invite.setStatus("ACCEPTED");
        invite.setUpdatedAt(LocalDateTime.now());
        groupInviteMapper.updateById(invite);

        joinGroup(userId, invite.getGroupId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectInvite(Long userId, Long inviteId) {
        GroupInvite invite = groupInviteMapper.selectById(inviteId);
        if (invite == null || invite.getDeleted() != null && invite.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "邀请不存在");
        }
        if (!userId.equals(invite.getInviteeId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权处理该邀请");
        }
        if (!"PENDING".equals(invite.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "邀请已处理");
        }

        invite.setStatus("REJECTED");
        invite.setUpdatedAt(LocalDateTime.now());
        groupInviteMapper.updateById(invite);
    }

    private GroupInfo ensureGroupExists(Long groupId) {
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null || (group.getDeleted() != null && group.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群聊不存在");
        }
        return group;
    }

    private GroupMember ensureMember(Long groupId, Long userId) {
        GroupMember member = groupMemberMapper.selectOne(Wrappers.<GroupMember>lambdaQuery()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不是该群成员");
        }
        return member;
    }
}
