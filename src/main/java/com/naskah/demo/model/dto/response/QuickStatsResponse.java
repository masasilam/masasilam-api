package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class QuickStatsResponse {
    private Integer totalBooks;
    private String readingTime;             // "24h 30m" formatted
    private Integer completedBooks;
    private Double averageRating;
    private Integer currentStreak;
    private Boolean hasActivityToday;
}
