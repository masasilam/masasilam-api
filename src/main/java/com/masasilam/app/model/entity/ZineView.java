package com.masasilam.app.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZineView {
    private Long id;
    private Long zineId;
    private String slug;
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private String viewerHash;
    private String actionType; // "view" | "download"
    private LocalDateTime createdAt;
}