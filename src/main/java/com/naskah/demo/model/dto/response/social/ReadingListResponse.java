package com.naskah.demo.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class ReadingListResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userPhoto;
    private String title;
    private String slug;
    private String description;
    private String coverImageUrl;
    private String visibility;
    private Boolean isFeatured;
    private Integer itemCount;
    private Integer likeCount;
    private Integer viewCount;
    private Integer forkCount;
    private Long forkedFromId;
    private String forkedFromTitle;
    private List<String> tags;

    // Current user context
    private Boolean isLikedByMe;
    private Boolean isFollowedByMe;
    private Boolean isOwner;

    // Preview items
    private List<ReadingListItemResponse> items;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}