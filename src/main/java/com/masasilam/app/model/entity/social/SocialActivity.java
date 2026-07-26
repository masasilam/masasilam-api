package com.masasilam.app.model.entity.social;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SocialActivity {
    private Long id;
    private Long userId;
    private String activityType;
    private String entityType;
    private Long entityId;
    private String entitySlug;
    private String entityTitle;
    private String entityCover;
    private Map<String, Object> metadata;
    private String visibility;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
}