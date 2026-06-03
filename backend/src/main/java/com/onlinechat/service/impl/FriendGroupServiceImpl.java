package com.onlinechat.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.onlinechat.common.FriendshipStatus;
import com.onlinechat.common.ResultCode;
import com.onlinechat.dto.FriendGroupCreateDTO;
import com.onlinechat.dto.FriendGroupUpdateDTO;
import com.onlinechat.entity.FriendGroup;
import com.onlinechat.entity.Friendship;
import com.onlinechat.exception.BusinessException;
import com.onlinechat.mapper.FriendGroupMapper;
import com.onlinechat.mapper.FriendshipMapper;
import com.onlinechat.service.FriendGroupService;
import com.onlinechat.vo.FriendGroupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendGroupServiceImpl implements FriendGroupService {

    private static final String DEFAULT_GROUP_NAME = "默认分组";

    private final FriendGroupMapper friendGroupMapper;
    private final FriendshipMapper friendshipMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendGroup getOrCreateDefaultGroup(Long userId) {
        FriendGroup group = friendGroupMapper.selectOne(Wrappers.<FriendGroup>lambdaQuery()
                .eq(FriendGroup::getUserId, userId)
                .eq(FriendGroup::getIsDefault, 1)
                .last("LIMIT 1"));
        if (group != null) {
            return group;
        }
        LocalDateTime now = LocalDateTime.now();
        group = new FriendGroup();
        group.setUserId(userId);
        group.setName(DEFAULT_GROUP_NAME);
        group.setIsDefault(1);
        group.setSortOrder(0);
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        friendGroupMapper.insert(group);
        return group;
    }

    @Override
    public List<FriendGroupVO> listGroups(Long userId) {
        getOrCreateDefaultGroup(userId);
        return friendGroupMapper.selectGroupsWithCount(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendGroupVO createGroup(Long userId, FriendGroupCreateDTO dto) {
        ensureNameAvailable(userId, dto.getName(), null);
        LocalDateTime now = LocalDateTime.now();
        Long groupCount = friendGroupMapper.selectCount(Wrappers.<FriendGroup>lambdaQuery()
                .eq(FriendGroup::getUserId, userId));

        FriendGroup group = new FriendGroup();
        group.setUserId(userId);
        group.setName(dto.getName().trim());
        group.setIsDefault(0);
        group.setSortOrder(groupCount.intValue() + 1);
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        friendGroupMapper.insert(group);
        return toVO(group, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendGroupVO updateGroup(Long userId, Long groupId, FriendGroupUpdateDTO dto) {
        FriendGroup group = requireOwnedGroup(userId, groupId);
        ensureNameAvailable(userId, dto.getName(), groupId);
        group.setName(dto.getName().trim());
        group.setUpdatedAt(LocalDateTime.now());
        friendGroupMapper.updateById(group);
        Long friendCount = friendshipMapper.selectCount(Wrappers.<Friendship>lambdaQuery()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getGroupId, groupId)
                .eq(Friendship::getStatus, FriendshipStatus.ACTIVE));
        return toVO(group, friendCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long userId, Long groupId) {
        FriendGroup group = requireOwnedGroup(userId, groupId);
        if (group.getIsDefault() != null && group.getIsDefault() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "默认分组不能删除");
        }
        FriendGroup defaultGroup = getOrCreateDefaultGroup(userId);

        Friendship update = new Friendship();
        update.setGroupId(defaultGroup.getId());
        update.setUpdatedAt(LocalDateTime.now());
        friendshipMapper.update(update, Wrappers.<Friendship>lambdaUpdate()
                .eq(Friendship::getUserId, userId)
                .eq(Friendship::getGroupId, groupId)
                .eq(Friendship::getStatus, FriendshipStatus.ACTIVE));
        friendGroupMapper.deleteById(groupId);
    }

    @Override
    public FriendGroup requireOwnedGroup(Long userId, Long groupId) {
        FriendGroup group = friendGroupMapper.selectOne(Wrappers.<FriendGroup>lambdaQuery()
                .eq(FriendGroup::getId, groupId)
                .eq(FriendGroup::getUserId, userId)
                .last("LIMIT 1"));
        if (group == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "好友分组不存在");
        }
        return group;
    }

    private void ensureNameAvailable(Long userId, String name, Long excludeGroupId) {
        Long count = friendGroupMapper.selectCount(Wrappers.<FriendGroup>lambdaQuery()
                .eq(FriendGroup::getUserId, userId)
                .eq(FriendGroup::getName, name.trim())
                .ne(excludeGroupId != null, FriendGroup::getId, excludeGroupId));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "分组名称已存在");
        }
    }

    private FriendGroupVO toVO(FriendGroup group, Long friendCount) {
        FriendGroupVO vo = new FriendGroupVO();
        vo.setId(group.getId());
        vo.setName(group.getName());
        vo.setIsDefault(group.getIsDefault());
        vo.setSortOrder(group.getSortOrder());
        vo.setFriendCount(friendCount);
        vo.setCreatedAt(group.getCreatedAt());
        return vo;
    }
}
