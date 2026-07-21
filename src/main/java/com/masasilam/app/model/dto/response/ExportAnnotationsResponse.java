package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExportAnnotationsResponse {
    private Long exportId;
    private String status;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String exportType;
    private Integer totalBookmarks;
    private Integer totalHighlights;
    private Integer totalNotes;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private String errorMessage;
}