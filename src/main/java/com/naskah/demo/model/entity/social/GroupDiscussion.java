package com.naskah.demo.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GroupDiscussion {
    private Long id;
    private Long groupId;
    private Long scheduleId;
    private Long userId;
    private Long parentId;
    private String title;
    private String content;
    private String entityType;
    private Long entityId;
    private String cfiReference;
    private Integer likeCount;
    private Integer replyCount;
    private Boolean isPinned;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}