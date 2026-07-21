package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ChapterAnalyticsResponse {
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterSlug;
    private Long totalReaders;
    private Long uniqueReaders;
    private Integer averageReadingTimeSeconds;
    private Double averageScrollDepth;
    private Double completionRate;
    private Double skipRate;
    private Double rereadRate;
    private Double averageRating;
    private Integer totalRatings;
    private RatingDistribution ratingDistribution;
    private Integer totalBookmarks;
    private Integer totalHighlights;
    private Integer totalNotes;
    private Integer totalComments;
    private Integer engagementScore;
    private String popularityLevel;
    private String difficultyLevel;
    private List<PopularHighlight> topHighlights;
    private ChapterComparison comparisonToPrevious;
    private ChapterComparison comparisonToAverage;
}