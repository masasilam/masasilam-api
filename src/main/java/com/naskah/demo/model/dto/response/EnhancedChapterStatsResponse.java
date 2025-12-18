package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class EnhancedChapterStatsResponse extends ChapterStatsResponse {
    // Add to existing ChapterStatsResponse

    // Rating stats
    private Double averageRating;
    private Integer totalRatings;
    private RatingDistribution ratingDistribution;

    // Reading patterns
    private Double skipRate;
    private Double rereadRate;
    private Integer averageSessionDuration;

    // Engagement details
    private Double engagementScore; // Calculated score 0-100
    private String popularityTrend; // "rising", "stable", "declining"
}
