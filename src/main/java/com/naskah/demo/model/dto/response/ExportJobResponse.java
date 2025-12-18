package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExportJobResponse {
    private Long exportId;
    private String format;
    private String status;                  // "pending", "processing", "completed", "failed"
    private String downloadUrl;
    private Long fileSizeBytes;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private String errorMessage;
}
