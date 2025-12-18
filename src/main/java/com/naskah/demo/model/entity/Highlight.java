package com.naskah.demo.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Highlight {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer chapterNumber;
    private String startPosition;
    private String endPosition;
    private String highlightedText;
    private String color;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}