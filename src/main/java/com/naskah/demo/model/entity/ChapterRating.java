package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterRating {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private Integer rating; // 1-5
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
