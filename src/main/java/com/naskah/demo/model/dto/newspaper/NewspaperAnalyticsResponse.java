package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperAnalyticsResponse {
    private LocalDate dateFrom;
    private LocalDate dateTo;

    // Overview
    private Long totalViews;
    private Long totalReads;
    private Integer totalShares;
    private Integer totalSaves;
    private Integer totalComments;

    // Popular articles
    private List<NewspaperArticleResponse> topViewedArticles;
    private List<NewspaperArticleResponse> topRatedArticles;
    private List<NewspaperArticleResponse> topSharedArticles;

    // Category performance
    private List<CategoryPerformance> categoryPerformance;

    // Source performance
    private List<SourcePerformance> sourcePerformance;

    // Trending topics
    private List<String> trendingTags;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryPerformance {
        private String category;
        private Integer articleCount;
        private Long viewCount;
        private BigDecimal averageRating;
        private Integer engagementScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourcePerformance {
        private String sourceName;
        private Integer articleCount;
        private Long viewCount;
        private BigDecimal averageRating;
    }
}