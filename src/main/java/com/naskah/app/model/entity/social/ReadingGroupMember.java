package com.naskah.app.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReadingGroupMember {
    private Long id;
    private Long groupId;
    private Long userId;
    private String role;  // owner, moderator, member
    private LocalDateTime joinedAt;
    private Boolean isActive;
}