package com.naskah.app.model.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ZineReadingProgressResponse {
    private Long    id;
    private Long    userId;
    private Long    zineId;
    private String  zineSlug;
    private String  zineTitle;
    private String  coverImageUrl;
    private String  currentPosition;    // epubcfi
    private Integer currentPage;        // chapter (1-based)
    private Integer totalPages;
    private BigDecimal percentageCompleted;
    private LocalDateTime lastReadAt;
    private LocalDateTime updatedAt;
}