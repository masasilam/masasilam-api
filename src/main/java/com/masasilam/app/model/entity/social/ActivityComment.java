package com.masasilam.app.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityComment {
    private Long id;
    private Long activityId;
    private Long userId;
    private Long parentId;
    private String content;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}