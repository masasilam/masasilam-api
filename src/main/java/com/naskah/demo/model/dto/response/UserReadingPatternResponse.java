package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserReadingPatternResponse {
    private Long bookId;
    private String bookTitle;

    // Time patterns
    private Integer preferredReadingHour;
    private String preferredReadingTime; // "Morning", "Afternoon", "Evening", "Night"
    private Integer preferredDayOfWeek;
    private String preferredDay; // "Monday", "Tuesday", etc.
    private Integer averageSessionDurationMinutes;

    // Behavior patterns
    private Double skipRate;
    private Double rereadRate;
    private Double completionSpeedChaptersPerDay;
    private String readingPace; // "Slow", "Moderate", "Fast"

    // Engagement patterns
    private Double annotationFrequency;
    private String annotationStyle; // "Heavy", "Moderate", "Light"
    private Integer averageReadingSpeedWpm;

    private LocalDateTime lastCalculatedAt;
}
