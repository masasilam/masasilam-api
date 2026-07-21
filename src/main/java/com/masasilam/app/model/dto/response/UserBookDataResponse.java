package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class UserBookDataResponse {
    private ReadingProgressSummary readingProgress;
    private List<BookmarkResponse> bookmarks;
    private List<HighlightResponse> highlights;
    private List<NoteResponse> notes;
    private List<ChapterRatingResponse> ratings;
    private Double myAverageRating;
    private ReadingHistorySummary readingHistory;
    private List<SearchHistoryResponse> recentSearches;
    private UserReadingPatternResponse patterns;
    private UserBookStatistics statistics;
}