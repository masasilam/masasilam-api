package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class AnalyticsOverview {
    private Long totalReaders;
    private Long activeReaders;
    private Long newReaders;
    private Double averageCompletionRate;
    private Integer averageReadingTimeMinutes;
    private Double averageRating;
    private Integer totalRatings;
    private Integer totalReviews;
}