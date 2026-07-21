package com.masasilam.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserReadingPattern {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer preferredReadingHour;
    private Integer preferredDayOfWeek;
    private Integer averageSessionDurationMinutes;
    private Double skipRate;
    private Double rereadRate;
    private Double completionSpeedChaptersPerDay;
    private Double annotationFrequency;
    private Integer averageReadingSpeedWpm;
    private LocalDateTime lastCalculatedAt;
    private LocalDateTime createdAt;
}