package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class DashboardMainResponse {
    private OverviewStats              overviewStats;
    private List<BookInProgressResponse> booksInProgress;
    private List<RecentlyReadResponse> recentlyRead;
    private ReadingPatternSummary      readingPattern;
    private AnnotationsSummary         annotationsSummary;
    private QuickAccessLinks           quickLinks;
    private List<RecentAchievementResponse> recentAchievements;
}
