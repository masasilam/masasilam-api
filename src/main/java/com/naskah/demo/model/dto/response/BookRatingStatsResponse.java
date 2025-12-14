package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class BookRatingStatsResponse {
    private Long bookId;
    private Double averageRating;
    private Long totalRatings;
    private Integer rating5Count;
    private Integer rating4Count;
    private Integer rating3Count;
    private Integer rating2Count;
    private Integer rating1Count;
}
