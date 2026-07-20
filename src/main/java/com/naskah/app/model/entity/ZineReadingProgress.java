package com.naskah.app.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZineReadingProgress {
    private Long id;
    private Long userId;
    private Long zineId;
    private String currentPosition; // epubcfi
    private Integer currentPage;
    private Integer totalPages;
    private BigDecimal percentageCompleted;
    private LocalDateTime lastReadAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}