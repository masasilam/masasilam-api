package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class FilmRatingStatsResponse {
    private Long filmId;
    private Double averageRating;
    private Long totalRatings;
    private Integer count50;
    private Integer count45;
    private Integer count40;
    private Integer count35;
    private Integer count30;
    private Integer count25;
    private Integer count20;
    private Integer count15;
    private Integer count10;
    private Integer count05;
    private Map<String, Integer> distribution;
}