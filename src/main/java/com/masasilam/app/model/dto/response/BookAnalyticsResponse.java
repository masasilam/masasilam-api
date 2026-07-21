package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookAnalyticsResponse {
    private Long bookId;
    private String bookTitle;
    private LocalDateTime analyzedAt;
    private String dateRange;
    private AnalyticsOverview overview;
    private ReaderBehaviorAnalytics readerBehavior;
    private ContentEngagementAnalytics contentEngagement;
    private List<PopularPassage> mostHighlightedPassages;
    private List<PopularNote> mostCommonNotes;
    private List<ChapterDropOffPoint> dropOffPoints;
    private List<ChapterSkipAnalysis> mostSkippedChapters;
    private TrendAnalysis trends;
}