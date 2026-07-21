package com.masasilam.app.model.entity.social;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChallengeProgressItem {
    private Long id;
    private Long participantId;
    private String entityType;
    private Long entityId;
    private String entityTitle;
    private String entitySlug;
    private String entityCover;
    private LocalDateTime completedAt;
}