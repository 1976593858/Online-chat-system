package com.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupVO {
    private Long id;
    private String name;
    private String announcement;
    private Long ownerId;
    private String ownerUsername;
    private String ownerNickname;
    private Long memberCount;
    private LocalDateTime createdAt;
}
