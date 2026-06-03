package com.onlinechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinechat.entity.FriendRequest;
import com.onlinechat.vo.FriendRequestVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

    @Select({
            "<script>",
            "SELECT",
            "  r.id,",
            "  r.sender_id AS senderId,",
            "  su.username AS senderUsername,",
            "  su.nickname AS senderNickname,",
            "  su.avatar AS senderAvatar,",
            "  r.receiver_id AS receiverId,",
            "  ru.username AS receiverUsername,",
            "  ru.nickname AS receiverNickname,",
            "  ru.avatar AS receiverAvatar,",
            "  r.message,",
            "  r.status,",
            "  r.handle_reason AS handleReason,",
            "  r.handled_at AS handledAt,",
            "  r.created_at AS createdAt",
            "FROM friend_request r",
            "JOIN `user` su ON su.id = r.sender_id AND su.deleted = 0",
            "JOIN `user` ru ON ru.id = r.receiver_id AND ru.deleted = 0",
            "WHERE r.deleted = 0",
            "<choose>",
            "  <when test='direction == \"sent\"'>AND r.sender_id = #{userId}</when>",
            "  <otherwise>AND r.receiver_id = #{userId}</otherwise>",
            "</choose>",
            "<if test='status != null and status != \"\"'>",
            "  AND r.status = #{status}",
            "</if>",
            "ORDER BY r.created_at DESC",
            "</script>"
    })
    IPage<FriendRequestVO> selectRequestPage(Page<FriendRequestVO> page,
                                             @Param("userId") Long userId,
                                             @Param("direction") String direction,
                                             @Param("status") String status);
}
