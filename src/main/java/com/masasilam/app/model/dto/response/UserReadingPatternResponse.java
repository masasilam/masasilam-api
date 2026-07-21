package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserReadingPatternResponse {
    private Long bookId;
    private String bookTitle;
    private Integer preferredReadingHour;
    private String preferredReadingTime;
    private Integer preferredDayOfWeek;
    private String preferredDay;
    private Integer averageSessionDurationMinutes;
    private Double skipRate;
    private Double rereadRate;
    private Double completionSpeedChaptersPerDay;
    private String readingPace;
    private Double annotationFrequency;
    private String annotationStyle;
    private Integer averageReadingSpeedWpm;
    private LocalDateTime lastCalculatedAt;
}