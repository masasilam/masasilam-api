package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookLibraryItemResponse {
    private Long bookId;
    private String bookTitle;
    private String bookSlug;
    private String coverImageUrl;
    private String authorName;
    private String genre;

    // Reading progress
    private Double progressPercentage;
    private Integer currentChapter;
    private Integer totalChapters;
    private String readingStatus;           // "reading", "completed", "not_started"

    // Engagement
    private Integer bookmarkCount;
    private Integer highlightCount;
    private Integer noteCount;
    private Double myRating;
    private Boolean hasReview;

    // Timestamps
    private LocalDateTime firstReadAt;
    private LocalDateTime lastReadAt;
    private LocalDateTime completedAt;

    // Stats
    private Integer totalReadingTimeMinutes;
    private Integer estimatedTimeRemaining;
}
