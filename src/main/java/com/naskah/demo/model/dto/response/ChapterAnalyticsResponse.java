package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ChapterAnalyticsResponse {
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterSlug;

    // Reading Stats
    private Long totalReaders;
    private Long uniqueReaders;
    private Integer averageReadingTimeSeconds;
    private Double averageScrollDepth;

    // Engagement
    private Double completionRate;
    private Double skipRate;
    private Double rereadRate;

    // Ratings
    private Double averageRating;
    private Integer totalRatings;
    private RatingDistribution ratingDistribution;

    // Annotations
    private Integer totalBookmarks;
    private Integer totalHighlights;
    private Integer totalNotes;
    private Integer totalComments;

    // Calculated Metrics
    private Integer engagementScore; // 0-100
    private String popularityLevel; // "Very High", "High", "Medium", "Low"
    private String difficultyLevel; // Based on skip rate, reading time

    // Top Highlights in this chapter
    private List<PopularHighlight> topHighlights;

    // Comparisons
    private ChapterComparison comparisonToPrevious;
    private ChapterComparison comparisonToAverage;
}