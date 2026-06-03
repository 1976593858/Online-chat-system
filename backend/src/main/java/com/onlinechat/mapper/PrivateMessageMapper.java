package com.onlinechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.onlinechat.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    @Update({
            "UPDATE private_message",
            "SET read_at = #{readAt}",
            "WHERE to_user_id = #{toUserId}",
            "  AND from_user_id = #{fromUserId}",
            "  AND read_at IS NULL",
            "  AND deleted = 0"
    })
    int markRead(@Param("toUserId") Long toUserId,
                 @Param("fromUserId") Long fromUserId,
                 @Param("readAt") LocalDateTime readAt);
}

