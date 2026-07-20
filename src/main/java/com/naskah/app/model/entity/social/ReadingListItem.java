package com.naskah.app.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReadingListItem {
    private Long id;
    private Long listId;
    private String entityType;
    private Long entityId;
    private String entitySlug;
    private String entityTitle;
    private String entityCover;
    private Long addedBy;
    private String note;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}