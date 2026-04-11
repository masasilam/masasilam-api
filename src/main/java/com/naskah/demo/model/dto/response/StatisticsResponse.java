package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class StatisticsResponse {
    private Integer                  totalBooksRead;
    private Integer                  totalChaptersRead;
    private Integer                  totalReadingMinutes;
    private Integer                  averageReadingSpeedWpm;
    private TrendData                readingTimeTrend;
    private TrendData                completionTrend;
    private TrendData                speedTrend;
    private List<GenreBreakdownItem> genreBreakdown;
    private List<PeakReadingTimeItem> peakReadingTimes;
}