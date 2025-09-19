package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class SearchResultResponse {
    private String query;
    private Integer totalResults;
    private Integer currentPage;
    private Integer totalPages;
    private List<SearchResult> results;

    @Data
    public static class SearchResult {
        private Integer page;
        private String context; // Text around the found term
        private String highlightedText; // The found text with highlighting
        private String position;
        private Double relevanceScore;
    }
}