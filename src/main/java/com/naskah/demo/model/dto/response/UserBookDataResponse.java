package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class UserBookDataResponse {
    // Reading Progress
    private ReadingProgressSummary readingProgress;

    // Annotations
    private List<BookmarkResponse> bookmarks;
    private List<HighlightResponse> highlights;
    private List<NoteResponse> notes;

    // Ratings
    private List<ChapterRatingResponse> ratings;
    private Double myAverageRating;

    // Reading Activity
    private ReadingHistorySummary readingHistory;

    // Search History
    private List<SearchHistoryResponse> recentSearches;

    // User Patterns
    private UserReadingPatternResponse patterns;

    // Statistics
    private UserBookStatistics statistics;
}
