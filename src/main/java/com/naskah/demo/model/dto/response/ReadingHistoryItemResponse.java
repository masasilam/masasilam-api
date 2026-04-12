package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingHistoryItemResponse {
    private Long activityId;
    private String activityType;
    private Long bookId;
    private String bookSlug;
    private String bookTitle;
    private String authorName;
    private String bookCover;
    private LocalDateTime timestamp;
    private String description;
    private Integer chapterNumber;
    private Double progressPercentage;
    private String lastCfi;
}