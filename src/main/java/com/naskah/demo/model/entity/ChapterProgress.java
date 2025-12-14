package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChapterProgress {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private Integer position; // Current reading position in the chapter
    private Integer readingTimeSeconds; // Total reading time in seconds
    private Boolean isCompleted;
    private LocalDateTime lastReadAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}