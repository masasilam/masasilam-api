package com.masasilam.app.model.dto.response;

import com.masasilam.app.model.dto.response.ZineDashboardDTOs.ZineInProgressResponse;
import lombok.Data;

import java.util.List;

@Data
public class DashboardMainResponse {
    private OverviewStats overviewStats;
    private List<BookInProgressResponse> booksInProgress;
    private List<ZineInProgressResponse> zinesInProgress;
    private ReadingPatternSummary readingPattern;
    private List<RecentlyReadResponse> recentlyRead;
    private AnnotationsSummary annotationsSummary;
    private List<Object> recentAchievements;
}