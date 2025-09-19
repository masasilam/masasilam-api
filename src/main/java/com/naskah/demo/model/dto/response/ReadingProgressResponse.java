package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReadingProgressResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private Integer currentPage;
    private Integer totalPages;
    private String currentPosition;
    private BigDecimal percentageCompleted;
    private Integer readingTimeMinutes;
    private String status;
    private LocalDateTime lastReadAt;
    private LocalDateTime startedAt;
}