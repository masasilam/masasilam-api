package com.masasilam.app.model.dto.response;

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
    private Double progressPercentage;
    private Integer currentChapter;
    private Integer totalChapters;
    private String readingStatus;
    private Integer bookmarkCount;
    private Integer highlightCount;
    private Integer noteCount;
    private Double myRating;
    private Boolean hasReview;
    private LocalDateTime firstReadAt;
    private LocalDateTime lastReadAt;
    private LocalDateTime completedAt;
    private Integer totalReadingTimeMinutes;
    private Integer estimatedTimeRemaining;
}