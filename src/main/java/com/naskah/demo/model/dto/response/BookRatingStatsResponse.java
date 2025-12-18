package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class BookRatingStatsResponse {
    private Long bookId;
    private Double averageRating;
    private Long totalRatings;
    private Integer rating50Count;
    private Integer rating45Count;
    private Integer rating40Count;
    private Integer rating35Count;
    private Integer rating30Count;
    private Integer rating25Count;
    private Integer rating20Count;
    private Integer rating15Count;
    private Integer rating10Count;
    private Integer rating05Count;
}
