package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChapterRatingResponse {
    private Long id;
    private Long userId;
    private Integer chapterNumber;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}