package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class BookRecommendationResponse {
    private Long bookId;
    private String bookTitle;
    private String bookSlug;
    private String coverImageUrl;
    private String authorName;
    private String genre;

    private Double averageRating;
    private Integer totalReaders;

    // Recommendation metadata
    private String recommendationReason;    // "Based on your love for fantasy"
    private Double matchScore;              // 0-100
    private List<String> matchingFactors;   // ["genre", "author", "rating"]
}
