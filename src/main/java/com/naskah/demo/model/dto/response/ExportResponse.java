package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExportResponse {
    private String fileName;
    private String downloadUrl;
    private String format;
    private Long fileSize;
    private Integer totalHighlights;
    private Integer totalNotes;
    private LocalDateTime generatedAt;
    private String expiresAt; // URL expiry time
}