package com.masasilam.app.model.dto.response.social;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class SocialAnnotationResponse {
    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private String userPhoto;
    private Boolean isVerified;

    private String entityType;
    private Long entityId;
    private String entitySlug;
    private String entityTitle;
    private String entityCover;

    private String cfi;
    private String selectedText;
    private String color;
    private String note;
    private String contextBefore;
    private String contextAfter;
    private String chapterLabel;
    private String visibility;

    private Integer likeCount;
    private Integer commentCount;
    private Integer reshareCount;
    private Boolean isLikedByMe;
    private Boolean isOwner;

    private List<AnnotationCommentResponse> recentComments;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String timeAgo;
}