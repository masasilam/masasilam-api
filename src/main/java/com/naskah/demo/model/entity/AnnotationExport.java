package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnotationExport {
    private Long id;
    private Long userId;
    private Long bookId;

    // Export configuration
    private String exportType; // PDF, DOCX, JSON, HTML, MD
    private Boolean includeBookmarks;
    private Boolean includeHighlights;
    private Boolean includeNotes;

    // Filtering
    private Integer chapterFrom;
    private Integer chapterTo;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    // Export metadata
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String fileUrl;
    private Long fileSize;
    private String fileName;

    // Statistics
    private Integer totalBookmarks;
    private Integer totalHighlights;
    private Integer totalNotes;

    // Processing
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}