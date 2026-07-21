package com.masasilam.app.model.dto.newspaper;

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
    private Long totalViews;
    private Long totalReads;
    private Integer totalShares;
    private Integer totalSaves;
    private Integer totalComments;
    private List<NewspaperArticleResponse> topViewedArticles;
    private List<NewspaperArticleResponse> topRatedArticles;
    private List<NewspaperArticleResponse> topSharedArticles;
    private List<CategoryPerformance> categoryPerformance;
    private List<SourcePerformance> sourcePerformance;
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