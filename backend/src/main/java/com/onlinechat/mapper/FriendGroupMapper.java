package com.onlinechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.onlinechat.entity.FriendGroup;
import com.onlinechat.vo.FriendGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendGroupMapper extends BaseMapper<FriendGroup> {

    @Select("""
            SELECT
                g.id,
                g.name,
                g.is_default AS isDefault,
                g.sort_order AS sortOrder,
                COUNT(f.id) AS friendCount,
                g.created_at AS createdAt
            FROM friend_group g
            LEFT JOIN friendship f
                ON f.group_id = g.id
                AND f.user_id = g.user_id
                AND f.status = 'ACTIVE'
                AND f.deleted = 0
            WHERE g.user_id = #{userId}
              AND g.deleted = 0
            GROUP BY g.id, g.name, g.is_default, g.sort_order, g.created_at
            ORDER BY g.is_default DESC, g.sort_order ASC, g.created_at ASC
            """)
    List<FriendGroupVO> selectGroupsWithCount(@Param("userId") Long userId);
}
