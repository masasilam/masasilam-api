package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class QuickStatsResponse {
    private Integer totalBooks;
    private String readingTime;
    private Integer completedBooks;
    private Double averageRating;
    private Integer currentStreak;
    private Boolean hasActivityToday;
}
