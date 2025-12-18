package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SearchHistoryResponse {
    private Long id;
    private String query;
    private Integer resultsCount;
    private LocalDateTime searchedAt;
    private Boolean wasClicked;
}
