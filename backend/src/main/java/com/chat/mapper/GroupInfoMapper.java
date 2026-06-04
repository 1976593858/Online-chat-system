package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chat.entity.GroupInfo;
import com.chat.vo.GroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupInfoMapper extends BaseMapper<GroupInfo> {

    @Select({
            "SELECT gi.id, gi.name, gi.announcement, gi.owner_id AS ownerId,",
            "  u.username AS ownerUsername, u.nickname AS ownerNickname,",
            "  (SELECT COUNT(*) FROM group_member gm WHERE gm.group_id = gi.id AND gm.deleted = 0) AS memberCount,",
            "  gi.created_at AS createdAt",
            "FROM group_info gi",
            "JOIN `user` u ON u.id = gi.owner_id AND u.deleted = 0",
            "WHERE gi.deleted = 0",
            "  AND gi.id IN (SELECT gm.group_id FROM group_member gm WHERE gm.user_id = #{userId} AND gm.deleted = 0)",
            "ORDER BY gi.created_at DESC"
    })
    List<GroupVO> selectUserGroups(@Param("userId") Long userId);

    @Select({
            "SELECT gi.id, gi.name, gi.announcement, gi.owner_id AS ownerId,",
            "  u.username AS ownerUsername, u.nickname AS ownerNickname,",
            "  (SELECT COUNT(*) FROM group_member gm WHERE gm.group_id = gi.id AND gm.deleted = 0) AS memberCount,",
            "  gi.created_at AS createdAt",
            "FROM group_info gi",
            "JOIN `user` u ON u.id = gi.owner_id AND u.deleted = 0",
            "WHERE gi.id = #{groupId} AND gi.deleted = 0"
    })
    GroupVO selectGroupDetail(@Param("groupId") Long groupId);
}
