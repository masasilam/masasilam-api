package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookAnalyticsResponse {
    private Long bookId;
    private String bookTitle;
    private LocalDateTime analyzedAt;
    private String dateRange;

    // Overview Stats
    private AnalyticsOverview overview;

    // Reader Behavior
    private ReaderBehaviorAnalytics readerBehavior;

    // Content Engagement
    private ContentEngagementAnalytics contentEngagement;

    // Popular Content
    private List<PopularPassage> mostHighlightedPassages;
    private List<PopularNote> mostCommonNotes;

    // Problem Areas
    private List<ChapterDropOffPoint> dropOffPoints;
    private List<ChapterSkipAnalysis> mostSkippedChapters;

    // Trends
    private TrendAnalysis trends;
}
