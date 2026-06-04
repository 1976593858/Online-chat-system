package com.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chat.entity.GroupMessage;
import com.chat.vo.GroupMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    @Select({
            "SELECT gm.id, gm.group_id AS groupId, gm.from_user_id AS fromUserId,",
            "  u.username AS fromUsername, u.nickname AS fromNickname, u.avatar AS fromAvatar,",
            "  gm.content, gm.message_type AS messageType, gm.created_at AS createdAt",
            "FROM group_message gm",
            "JOIN `user` u ON u.id = gm.from_user_id AND u.deleted = 0",
            "WHERE gm.group_id = #{groupId} AND gm.deleted = 0",
            "ORDER BY gm.created_at DESC"
    })
    IPage<GroupMessageVO> selectPageByGroupId(Page<GroupMessageVO> page, @Param("groupId") Long groupId);

    @Select({
            "SELECT gm.id, gm.group_id AS groupId, gm.from_user_id AS fromUserId,",
            "  u.username AS fromUsername, u.nickname AS fromNickname,",
            "  gm.content, gm.message_type AS messageType, gm.created_at AS createdAt",
            "FROM group_message gm",
            "JOIN `user` u ON u.id = gm.from_user_id AND u.deleted = 0",
            "WHERE gm.group_id = #{groupId} AND gm.deleted = 0",
            "ORDER BY gm.created_at ASC, gm.id ASC"
    })
    List<GroupMessageVO> selectAllByGroupId(@Param("groupId") Long groupId);
}
