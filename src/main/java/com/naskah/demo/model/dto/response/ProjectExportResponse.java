package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectExportResponse {
    private String projectTitle;
    private String exportFormat;
    private String downloadUrl;
    private Long fileSize;
    private LocalDateTime exportedAt;
    private LocalDateTime expiresAt;
}