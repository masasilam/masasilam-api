package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ReadingHistoryResponse {
    private Long bookId;
    private String bookTitle;
    private List<ReadingActivitySummary> activities;
    private ReadingStatistics statistics;
}
