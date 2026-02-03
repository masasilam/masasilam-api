package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Highlight {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterSlug;
    private Integer startPosition;
    private Integer endPosition;
    private String highlightedText;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}