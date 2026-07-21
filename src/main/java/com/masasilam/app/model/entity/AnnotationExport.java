package com.masasilam.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnotationExport {
    private Long id;
    private Long userId;
    private Long bookId;
    private String exportType;
    private Boolean includeBookmarks;
    private Boolean includeHighlights;
    private Boolean includeNotes;
    private Integer chapterFrom;
    private Integer chapterTo;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private String status;
    private String fileUrl;
    private Long fileSize;
    private String fileName;
    private Integer totalBookmarks;
    private Integer totalHighlights;
    private Integer totalNotes;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}