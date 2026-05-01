package com.naskah.demo.model.entity.social;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class SocialAnnotation {
    private Long id;
    private Long userId;
    private Long sourceAnnotationId;
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
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}