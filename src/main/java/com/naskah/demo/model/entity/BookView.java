package com.naskah.demo.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookView {
    private Long id;
    private Long bookId;
    private String slug;
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private String viewerHash;
    private String actionType;
    private LocalDateTime viewedAt;
}