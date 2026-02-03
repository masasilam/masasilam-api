package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnotationItemResponse {
    private Long id;
    private String type;                    // "bookmark", "highlight", "note"
    private String content;

    // Book info
    private Long bookId;
    private String bookTitle;
    private String bookSlug;
    private String bookCover;

    // Location
    private Integer chapterNumber;
    private String chapterTitle;
    private Integer startPosition;
    private Integer endPosition;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
