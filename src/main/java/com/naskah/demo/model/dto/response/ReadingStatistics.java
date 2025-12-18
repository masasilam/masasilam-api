package com.naskah.demo.model.dto.response;

import lombok.Data;

@Data
public class ReadingStatistics {
    private Integer totalChaptersRead;
    private Integer totalReadingTimeMinutes;
    private Integer averageReadingSpeedWpm;
    private Double completionRate;
    private Integer currentStreak; // days
    private Integer longestStreak;
}
