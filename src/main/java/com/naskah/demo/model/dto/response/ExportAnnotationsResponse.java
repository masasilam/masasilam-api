package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExportAnnotationsResponse {
    private Long exportId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String fileUrl; // Available when COMPLETED
    private String fileName;
    private Long fileSize; // bytes
    private String exportType;

    // Statistics
    private Integer totalBookmarks;
    private Integer totalHighlights;
    private Integer totalNotes;

    // Metadata
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private String errorMessage; // If FAILED
}
