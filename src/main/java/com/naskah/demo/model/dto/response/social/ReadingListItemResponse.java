package com.naskah.demo.model.dto.response.social;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ReadingListItemResponse {
    private Long id;
    private String entityType;
    private Long entityId;
    private String entitySlug;
    private String entityTitle;
    private String entityCover;
    private String note;
    private Integer sortOrder;
    private OffsetDateTime addedAt;
}