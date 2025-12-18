package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class AnalyticsOverview {
    private Long totalReaders;
    private Long activeReaders; // Last 30 days
    private Long newReaders; // This period
    private Double averageCompletionRate;
    private Integer averageReadingTimeMinutes;
    private Double averageRating;
    private Integer totalRatings;
    private Integer totalReviews;
}