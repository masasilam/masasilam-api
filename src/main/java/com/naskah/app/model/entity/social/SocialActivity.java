package com.naskah.app.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

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
    private String metadata;     // JSON string
    private String visibility;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
