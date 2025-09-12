package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReadingResponse {
    private Long bookId;
    private String title;
    private String slug;
    private String fileUrl;
    private Integer currentPage;
    private Integer totalPages;
    private String currentPosition;
    private BigDecimal percentageCompleted;
    private Integer readingTimeMinutes;
    private String status;
    private Boolean isFavorite;
    private Long sessionId;
}
