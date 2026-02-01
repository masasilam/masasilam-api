package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HighlightResponse {
    private Long id;
    private Long bookId;
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterSlug;
    private String startPosition;
    private String endPosition;
    private String highlightedText;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}