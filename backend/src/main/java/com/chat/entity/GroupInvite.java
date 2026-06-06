package com.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_invite")
public class GroupInvite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long senderId;
    private Long inviteeId;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
