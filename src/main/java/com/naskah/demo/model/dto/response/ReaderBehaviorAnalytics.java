package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class ReaderBehaviorAnalytics {
    // Device breakdown
    private Map<String, Integer> readersByDevice; // desktop: 100, mobile: 50

    // Time patterns
    private Map<Integer, Integer> readingHourDistribution; // hour -> count
    private Map<String, Integer> readingDayDistribution; // day -> count

    // Reading patterns
    private Double averageSessionDurationMinutes;
    private Double averageReadingSpeedWpm;
    private Integer averageChaptersPerSession;

    // Engagement patterns
    private Double annotationRate; // % of readers who annotate
    private Double ratingRate; // % of readers who rate
    private Double reviewRate; // % of readers who review
}
