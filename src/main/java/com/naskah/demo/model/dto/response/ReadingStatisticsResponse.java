package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ReadingStatisticsResponse {
    private String period;                  // "Last 30 days", "This month", etc.

    // Time series data
    private List<DailyReadingData> dailyStats;
    private List<WeeklyReadingData> weeklyStats;

    // Aggregate stats
    private Integer totalBooksRead;
    private Integer totalChaptersRead;
    private Integer totalReadingMinutes;
    private Double averageReadingSpeedWpm;

    // Trends
    private TrendData readingTimeTrend;     // "increasing", "stable", "decreasing"
    private TrendData completionTrend;
    private TrendData speedTrend;

    // Genre breakdown
    private List<GenreStats> genreBreakdown;

    // Peak times
    private List<TimeSlotStats> peakReadingTimes;

    @Data
    public static class DailyReadingData {
        private String date;                // "2025-01-15"
        private Integer minutesRead;
        private Integer chaptersCompleted;
        private Integer sessionsCount;
        private Boolean hadActivity;        // For calendar visualization
    }

    @Data
    public static class WeeklyReadingData {
        private String weekStart;           // "2025-01-08"
        private String weekEnd;
        private Integer minutesRead;
        private Integer booksCompleted;
        private Integer averageDailyMinutes;
    }

    @Data
    public static class TrendData {
        private String direction;           // "up", "down", "stable"
        private Double changePercentage;
        private String interpretation;      // Human-readable
    }

    @Data
    public static class GenreStats {
        private String genreName;
        private Integer booksRead;
        private Integer minutesSpent;
        private Double percentage;
        private Double averageRating;
    }

    @Data
    public static class TimeSlotStats {
        private Integer hour;               // 0-23
        private String label;               // "Morning", "Afternoon", etc.
        private Integer sessionsCount;
        private Integer minutesRead;
        private Double percentage;
    }
}