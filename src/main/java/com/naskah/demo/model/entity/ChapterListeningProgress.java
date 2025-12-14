package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterListeningProgress {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private Long currentPosition; // seconds
    private Boolean isCompleted;
    private LocalDateTime lastListenedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}