package com.onlinechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.onlinechat.entity.Conversation;
import com.onlinechat.vo.ConversationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select({
            "SELECT",
            "  c.id,",
            "  c.target_user_id AS targetUserId,",
            "  u.username AS targetUsername,",
            "  u.nickname AS targetNickname,",
            "  u.avatar AS targetAvatar,",
            "  c.conversation_type AS conversationType,",
            "  c.last_message_id AS lastMessageId,",
            "  c.last_message_content AS lastMessageContent,",
            "  c.last_message_type AS lastMessageType,",
            "  c.unread_count AS unreadCount,",
            "  c.pinned,",
            "  c.muted,",
            "  c.last_message_at AS lastMessageAt",
            "FROM conversation c",
            "LEFT JOIN `user` u ON u.id = c.target_user_id AND u.deleted = 0",
            "WHERE c.owner_id = #{ownerId}",
            "  AND c.deleted = 0",
            "ORDER BY c.pinned DESC, c.last_message_at DESC, c.updated_at DESC"
    })
    IPage<ConversationVO> selectRecentPage(Page<ConversationVO> page, @Param("ownerId") Long ownerId);

    @Select({
            "SELECT",
            "  c.id,",
            "  c.target_user_id AS targetUserId,",
            "  u.username AS targetUsername,",
            "  u.nickname AS targetNickname,",
            "  u.avatar AS targetAvatar,",
            "  c.conversation_type AS conversationType,",
            "  c.last_message_id AS lastMessageId,",
            "  c.last_message_content AS lastMessageContent,",
            "  c.last_message_type AS lastMessageType,",
            "  c.unread_count AS unreadCount,",
            "  c.pinned,",
            "  c.muted,",
            "  c.last_message_at AS lastMessageAt",
            "FROM conversation c",
            "LEFT JOIN `user` u ON u.id = c.target_user_id AND u.deleted = 0",
            "WHERE c.owner_id = #{ownerId}",
            "  AND c.target_user_id = #{targetUserId}",
            "  AND c.deleted = 0",
            "LIMIT 1"
    })
    ConversationVO selectPrivateOne(@Param("ownerId") Long ownerId, @Param("targetUserId") Long targetUserId);
}
