package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterReview {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private String content;
    private Long parentId; // For replies
    private Integer likeCount;
    private Boolean isSpoiler; // Mark as spoiler
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}