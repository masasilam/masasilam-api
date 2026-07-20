package com.naskah.app.model.dto.response.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChallengeProgressItemResponse {
    private Long id;
    private String entityType;
    private Long entityId;
    private String entityTitle;
    private String entitySlug;
    private String entityCover;
    private LocalDateTime completedAt;
}