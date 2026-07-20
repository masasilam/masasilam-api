package com.naskah.app.model.entity.social;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChallengeParticipant {
    private Long id;
    private Long challengeId;
    private Long userId;
    private String status;  // in_progress, completed, abandoned
    private Integer progressCount;
    private LocalDateTime completedAt;
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;
}