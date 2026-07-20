package com.naskah.app.model.dto.response.social;

import lombok.Data;

@Data
public class TimeCapsuleContentResponse {
    private Long entityId;
    private String entityType;
    private String entityTitle;
    private String entitySlug;
    private String entityCover;
    private String category;
    private String excerpt;
    private Integer readerCount;
}