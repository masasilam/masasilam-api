package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class BookmarkSuggestionResponse {
    private Integer page;
    private String title;
    private String description;
    private String reason;
    private Double relevanceScore;
    private String suggestedColor;
}