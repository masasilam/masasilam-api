package com.naskah.demo.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchHistory {
    private Long id;
    private Long userId;
    private Long bookId;

    // Search details
    private String query;
    private Integer resultsCount;

    // Context
    private String searchType; // in_book, global, author, etc.
    private String filters; // JSON string

    // Interaction
    private Long clickedResultId;
    private LocalDateTime clickedAt;

    private LocalDateTime createdAt;
}
