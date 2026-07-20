package com.naskah.app.model.entity.social;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ReadingList {
    private Long id;
    private Long userId;
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
    private String tags;   // stored as array text
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}