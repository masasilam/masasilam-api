package com.naskah.demo.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReadingProgress {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer currentPage;
    private Integer totalPages;
    private String currentPosition;
    private BigDecimal percentageCompleted;
    private Integer readingTimeMinutes;
    private String status;
    private Boolean isFavorite;
    private String notes;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastReadAt;
}
