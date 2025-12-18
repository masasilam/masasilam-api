package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserReadingPattern {
    private Long id;
    private Long userId;
    private Long bookId;

    // Time patterns
    private Integer preferredReadingHour; // 0-23
    private Integer preferredDayOfWeek; // 1-7
    private Integer averageSessionDurationMinutes;

    // Behavior patterns
    private Double skipRate; // % of chapters skipped
    private Double rereadRate; // % of chapters reread
    private Double completionSpeedChaptersPerDay;

    // Engagement patterns
    private Double annotationFrequency; // Annotations per chapter
    private Integer averageReadingSpeedWpm;

    private LocalDateTime lastCalculatedAt;
    private LocalDateTime createdAt;
}