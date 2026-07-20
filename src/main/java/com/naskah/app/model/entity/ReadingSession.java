package com.naskah.app.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

// ReadingSession.java
@Data
public class ReadingSession {
    private Long id;
    private Long userId;
    private Long bookId;
    private Long zineId;
    private String sessionId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer totalDurationSeconds;
    private Integer startChapter;
    private Integer endChapter;
    private Integer chaptersRead;
    private String sessionType;
    private Double completionDelta;
    private Integer totalInteractions;
    private String deviceType;
    private String deviceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}