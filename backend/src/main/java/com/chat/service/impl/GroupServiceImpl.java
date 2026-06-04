package com.chat.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chat.dto.GroupCreateDTO;
import com.chat.entity.GroupInfo;
import com.chat.entity.GroupMember;
import com.chat.entity.GroupMessage;
import com.chat.mapper.GroupInfoMapper;
import com.chat.mapper.GroupMemberMapper;
import com.chat.mapper.GroupMessageMapper;
import com.chat.service.GroupService;
import com.chat.vo.GroupMemberVO;
import com.chat.vo.GroupMessageVO;
import com.chat.vo.GroupVO;
import com.onlinechat.common.PageResult;
import com.onlinechat.common.ResultCode;
import com.onlinechat.entity.User;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private static final DateTimeFormatter EXPORT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GroupInfoMapper groupInfoMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageMapper groupMessageMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO createGroup(Long ownerId, GroupCreateDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        GroupInfo group = new GroupInfo();
        group.setName(dto.getName().trim());
        group.setAnnouncement(dto.getAnnouncement());
        group.setOwnerId(ownerId);
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        groupInfoMapper.insert(group);

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(ownerId);
        member.setRole("OWNER");
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        groupMemberMapper.insert(member);

        GroupVO vo = new GroupVO();
        vo.setId(group.getId());
        vo.setName(group.getName());
        vo.setAnnouncement(group.getAnnouncement());
        vo.setOwnerId(ownerId);
        vo.setMemberCount(1L);
        vo.setCreatedAt(now);
        return vo;
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
    @Transactional(rollbackFor = Exception.class)
    public void joinGroup(Long userId, Long groupId) {
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null || group.getDeleted() != null && group.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群聊不存在");
        }
        if (groupMemberMapper.isMember(groupId, userId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole("MEMBER");
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        groupMemberMapper.insert(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(Long userId, Long groupId) {
        GroupMember member = groupMemberMapper.selectOne(Wrappers.<GroupMember>lambdaQuery()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "你不在该群中");
        }
        groupMemberMapper.deleteById(member);

        long remainingCount = groupMemberMapper.selectCount(Wrappers.<GroupMember>lambdaQuery()
                .eq(GroupMember::getGroupId, groupId));
        if (remainingCount == 0) {
            groupInfoMapper.deleteById(groupId);
        }
    }

    @Override
    public List<GroupMemberVO> getGroupMembers(Long groupId) {
        ensureGroupExists(groupId);
        return groupMemberMapper.selectMembersByGroupId(groupId);
    }

    @Override
    public PageResult<GroupMessageVO> getGroupMessages(Long groupId, long pageNo, long pageSize) {
        ensureGroupExists(groupId);
        IPage<GroupMessageVO> page = groupMessageMapper.selectPageByGroupId(
                new Page<>(pageNo, pageSize), groupId);
        return PageResult.of(page);
    }

    @Override
    public byte[] exportGroupMessages(Long groupId) {
        GroupVO group = getGroupDetail(groupId);
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

    private void ensureGroupExists(Long groupId) {
        GroupInfo group = groupInfoMapper.selectById(groupId);
        if (group == null || group.getDeleted() != null && group.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "群聊不存在");
        }
    }
}
