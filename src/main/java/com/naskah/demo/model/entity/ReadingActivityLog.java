package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingActivityLog {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;

    // Session info
    private String sessionId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;

    // Reading behavior
    private Integer startPosition;
    private Integer endPosition;
    private Double scrollDepthPercentage;

    // Engagement metrics
    private Integer wordsRead;
    private Integer readingSpeedWpm;

    // Pattern detection
    private Boolean isSkip;
    private Boolean isReread;
    private Integer interactionCount;

    // Device info
    private String deviceType; // mobile, tablet, desktop
    private String source; // web, app, api

    private LocalDateTime createdAt;
}
