package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ReadingCalendarResponse {
    private Integer year;
    private Integer month;
    private List<CalendarDay> days;
    private CalendarStats stats;

    @Data
    public static class CalendarDay {
        private String date;                // "2025-01-15"
        private Boolean hasActivity;
        private Integer minutesRead;
        private Integer intensity;          // 0-4 for visualization
        private List<String> activities;    // Brief activity descriptions
    }

    @Data
    public static class CalendarStats {
        private Integer daysWithActivity;
        private Integer totalDays;
        private Double activityPercentage;
        private Integer totalMinutes;
        private Integer longestStreakInPeriod;
    }
}
