package com.masasilam.app.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchHistory {
    private Long id;
    private Long userId;
    private Long bookId;
    private String query;
    private Integer resultsCount;
    private String searchType;
    private String filters;
    private Long clickedResultId;
    private LocalDateTime clickedAt;
    private LocalDateTime createdAt;
}
