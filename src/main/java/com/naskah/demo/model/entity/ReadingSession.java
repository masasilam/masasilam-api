package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingSession {
    private Long id;
    private Long userId;
    private Long bookId;
    private String sessionId;

    // Session summary
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer totalDurationSeconds;
    private Integer chaptersRead;

    // Progress
    private Integer startChapter;
    private Integer endChapter;
    private Double completionDelta; // % progress made in this session

    // Engagement
    private Integer totalInteractions;

    // Device
    private String deviceType;
    private String deviceId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
