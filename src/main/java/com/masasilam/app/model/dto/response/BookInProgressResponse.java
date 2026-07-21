package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookInProgressResponse {
    private Long bookId;
    private String bookSlug;
    private String bookTitle;
    private String authorName;
    private String coverImageUrl;
    private double progressPercentage;
    private LocalDateTime lastReadAt;
    private Integer currentChapter;
    private Integer totalChapters;
    private String chapterLabel;
    private boolean isEpub;
    private String lastCfi;
}