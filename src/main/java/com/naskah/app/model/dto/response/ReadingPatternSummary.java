package com.naskah.app.model.dto.response;

import lombok.Data;

@Data
public class ReadingPatternSummary {
    private String  preferredReadingTime;
    private String  preferredDay;
    private Integer averageSessionMinutes;
    private Integer averageReadingSpeedWpm;
    private Double  estimatedReadingSpeedWpm;
    private Integer currentStreak;
    private Integer longestStreak;
    private String  readingPace;
}
