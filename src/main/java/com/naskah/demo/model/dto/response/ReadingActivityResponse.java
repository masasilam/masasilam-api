package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingActivityResponse {
    private Long activityId;
    private String activityType;            // "start_reading", "finish_chapter", "add_bookmark", etc.
    private String description;             // Human-readable description
    private LocalDateTime timestamp;

    // Related entities
    private Long bookId;
    private String bookTitle;
    private String bookSlug;
    private String bookCover;

    private Integer chapterNumber;
    private String chapterTitle;

    // Additional data based on activity type
    private String additionalData;          // JSON string with extra info
}
