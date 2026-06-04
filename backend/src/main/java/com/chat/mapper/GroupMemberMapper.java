package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.entity.GroupMember;
import com.chat.vo.GroupMemberVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    @Select({
            "SELECT gm.id, gm.group_id AS groupId, gm.user_id AS userId,",
            "  u.username, u.nickname, u.avatar,",
            "  gm.role, gm.joined_at AS joinedAt",
            "FROM group_member gm",
            "JOIN `user` u ON u.id = gm.user_id AND u.deleted = 0",
            "WHERE gm.group_id = #{groupId} AND gm.deleted = 0",
            "ORDER BY gm.role = 'OWNER' DESC, gm.joined_at ASC"
    })
    List<GroupMemberVO> selectMembersByGroupId(@Param("groupId") Long groupId);

    @Select({
            "SELECT COUNT(*) > 0",
            "FROM group_member",
            "WHERE group_id = #{groupId} AND user_id = #{userId} AND deleted = 0"
    })
    boolean isMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
