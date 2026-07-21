package com.masasilam.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterProgress {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private Integer position;
    private Integer readingTimeSeconds;
    private Boolean isCompleted;
    private LocalDateTime lastReadAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}