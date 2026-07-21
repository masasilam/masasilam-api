package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsResponse {
    private int totalBooksRead;
    private int totalChaptersRead;
    private int totalEpubChaptersRead;
    private int totalReadingMinutes;
    private double averageReadingSpeedWpm;
    private double estimatedReadingSpeedWpm;
    private TrendData readingTimeTrend;
    private TrendData completionTrend;
    private TrendData speedTrend;
    private List<GenreBreakdownItem> genreBreakdown;
    private List<PeakReadingTimeItem> peakReadingTimes;
}