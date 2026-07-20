package com.naskah.app.model.dto.response.social;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ActivityFeedItemResponse {
    private Long id;

    // User info
    private Long userId;
    private String username;
    private String displayName;
    private String userPhoto;
    private Boolean isVerified;

    // Activity info
    private String activityType;
    private String entityType;
    private Long entityId;
    private String entitySlug;
    private String entityTitle;
    private String entityCover;
    private Map<String, Object> metadata;

    // Engagement
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isLikedByMe;
    private List<ActivityCommentResponse> recentComments;

    private OffsetDateTime createdAt;
    private String timeAgo;  // "2 jam lalu"
}