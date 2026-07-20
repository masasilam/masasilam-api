package com.naskah.app.model.entity.social;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SocialAnnotationComment {
    private Long id;
    private Long annotationId;
    private Long userId;
    private Long parentId;
    private String content;
    private Boolean isDeleted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}