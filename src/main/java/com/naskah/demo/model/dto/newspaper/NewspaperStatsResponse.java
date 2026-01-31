package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewspaperStatsResponse {
    private Integer totalArticles;
    private Integer totalSources;
    private Integer totalCategories;
    private Long totalViews;
    private Integer articlesToday;
    private Integer articlesThisWeek;
    private Integer articlesThisMonth;

    // Category breakdown
    private List<CategoryStats> categoryStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private String category;
        private String categoryName;
        private Integer articleCount;
        private Long viewCount;
    }
}