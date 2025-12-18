package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadingHistorySummary {
    private Integer totalSessions;
    private Integer totalReadingTimeMinutes;
    private LocalDateTime firstReadAt;
    private LocalDateTime lastReadAt;
    private Integer currentStreak;
    private Integer longestStreak;
}
