package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class OverviewStats {
    private Integer totalBooks;
    private Integer booksInProgress;
    private Integer booksCompleted;
    private Integer totalReadingTimeHours;
    private Double  averageRating;
    private Integer currentStreak;
    private Integer longestStreak;
    private Double  completionRate;
}