package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LibraryBookResponse {
    private Long bookId;
    private String bookSlug;
    private String bookTitle;
    private String authorName;
    private String coverImageUrl;
    private Double progressPercentage;
    private String readingStatus;
    private LocalDateTime lastReadAt;
    private Integer totalChapters;
    private Integer currentChapter;
    private Integer bookmarkCount;
    private Integer highlightCount;
    private Integer noteCount;
    private String lastCfi;
}