package com.masasilam.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterRating {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
