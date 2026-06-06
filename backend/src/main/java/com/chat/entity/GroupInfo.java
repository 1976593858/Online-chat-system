package com.chat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_info")
public class GroupInfo {
    @TableId
    private Long id;
    private String name;
    private String announcement;
    private String inviteCode;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
