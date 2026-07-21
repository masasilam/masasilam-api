package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnotationItemResponse {
    private Long id;
    private String type;
    private String content;
    private Long bookId;
    private String bookTitle;
    private String bookSlug;
    private String bookCover;
    private Integer chapterNumber;
    private String chapterTitle;
    private Integer startPosition;
    private Integer endPosition;
    private String cfi;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}