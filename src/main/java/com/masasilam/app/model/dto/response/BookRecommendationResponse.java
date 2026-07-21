package com.masasilam.app.model.dto.response;

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
    private String recommendationReason;
    private Double matchScore;
    private List<String> matchingFactors;
}