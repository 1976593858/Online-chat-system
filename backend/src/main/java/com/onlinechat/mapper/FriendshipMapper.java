package com.onlinechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinechat.entity.Friendship;
import com.onlinechat.vo.FriendDetailVO;
import com.onlinechat.vo.FriendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {

    @Select({
            "<script>",
            "SELECT",
            "  f.id AS friendshipId,",
            "  f.friend_id AS friendId,",
            "  u.username,",
            "  u.nickname,",
            "  u.email,",
            "  u.avatar,",
            "  f.group_id AS groupId,",
            "  g.name AS groupName,",
            "  f.remark,",
            "  f.created_at AS createdAt",
            "FROM friendship f",
            "JOIN `user` u ON u.id = f.friend_id AND u.deleted = 0",
            "LEFT JOIN friend_group g ON g.id = f.group_id AND g.deleted = 0",
            "WHERE f.user_id = #{userId}",
            "  AND f.status = 'ACTIVE'",
            "  AND f.deleted = 0",
            "<if test='groupId != null'>",
            "  AND f.group_id = #{groupId}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  AND (u.username LIKE CONCAT('%', #{keyword}, '%')",
            "       OR u.nickname LIKE CONCAT('%', #{keyword}, '%')",
            "       OR f.remark LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY COALESCE(NULLIF(f.remark, ''), u.nickname, u.username) ASC, f.created_at DESC",
            "</script>"
    })
    IPage<FriendVO> selectFriendPage(Page<FriendVO> page,
                                     @Param("userId") Long userId,
                                     @Param("groupId") Long groupId,
                                     @Param("keyword") String keyword);

    @Select("""
            SELECT
                f.id AS friendshipId,
                f.friend_id AS friendId,
                u.username,
                u.nickname,
                u.email,
                u.phone,
                u.avatar,
                f.group_id AS groupId,
                g.name AS groupName,
                f.remark,
                f.created_at AS friendSince,
                u.last_login_at AS lastLoginAt
            FROM friendship f
            JOIN `user` u ON u.id = f.friend_id AND u.deleted = 0
            LEFT JOIN friend_group g ON g.id = f.group_id AND g.deleted = 0
            WHERE f.user_id = #{userId}
              AND f.friend_id = #{friendId}
              AND f.status = 'ACTIVE'
              AND f.deleted = 0
            LIMIT 1
            """)
    FriendDetailVO selectFriendDetail(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
