package com.masasilam.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterReview {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private String content;
    private Long parentId;
    private Integer likeCount;
    private Boolean isSpoiler;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}