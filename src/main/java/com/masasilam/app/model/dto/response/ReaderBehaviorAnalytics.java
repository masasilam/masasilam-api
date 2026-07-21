package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class ReaderBehaviorAnalytics {
    private Map<String, Integer> readersByDevice;
    private Map<Integer, Integer> readingHourDistribution;
    private Map<String, Integer> readingDayDistribution;
    private Double averageSessionDurationMinutes;
    private Double averageReadingSpeedWpm;
    private Integer averageChaptersPerSession;
    private Double annotationRate;
    private Double ratingRate;
    private Double reviewRate;
}