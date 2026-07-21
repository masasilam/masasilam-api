package com.masasilam.app.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public final class ZineDashboardDTOs {
    private ZineDashboardDTOs() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZineInProgressResponse {
        private Long zineId;
        private String zineSlug;
        private String zineTitle;
        private String authorName;
        private String coverImageUrl;
        private Double progressPercentage;
        private LocalDateTime lastReadAt;
        private Integer currentChapter;
        private Integer totalChapters;
        private String lastCfi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentZineResponse {
        private Long zineId;
        private String zineSlug;
        private String zineTitle;
        private String authorName;
        private String coverImageUrl;
        private LocalDateTime lastReadAt;
        private String activityType;
        private Integer currentChapter;
        private Integer totalChapters;
        private Double progressPercentage;
        private String lastCfi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZineLibraryItemResponse {
        private Long zineId;
        private String zineSlug;
        private String zineTitle;
        private String authorName;
        private String coverImageUrl;
        private Double progressPercentage;
        private String readingStatus;
        private LocalDateTime lastReadAt;
        private Integer bookmarkCount;
        private Integer highlightCount;
        private Integer currentChapter;
        private Integer totalChapters;
        private String lastCfi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZineLibraryPageResponse {
        private List<ZineLibraryItemResponse> items;
        private int totalData;
        private int page;
        private int limit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZineHistoryItemResponse {
        private Long activityId;
        private Long zineId;
        private String zineSlug;
        private String zineTitle;
        private String authorName;
        private String zineCover;
        private LocalDateTime timestamp;
        private Integer chapterNumber;
        private String description;
        private Double progressPercentage;
        private String lastCfi;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZineHistoryPageResponse {
        private List<ZineHistoryItemResponse> list;
        private int total;
        private int page;
        private int limit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZineStatisticsResponse {
        private int totalZinesRead;
        private int totalChaptersRead;
        private int totalReadingMinutes;
        private double estimatedReadingSpeedWpm;
        private int completedZines;
        private double completionRate;
        private TrendData readingTimeTrend;
        private TrendData completionTrend;
        private List<GenreBreakdownItem> genreBreakdown;
        private List<PeakReadingTimeItem> peakReadingTimes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombinedOverviewStats {
        private int totalBooks;
        private int completedBooks;
        private int totalBookReadingMinutes;
        private int totalZines;
        private int completedZines;
        private int totalZineReadingMinutes;
        private int totalReadingMinutes;
        private int currentStreak;
        private int longestStreak;
        private double averageRating;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserZineReviewItemResponse {
        private Long reviewId;
        private Long zineId;
        private String zineTitle;
        private String zineSlug;
        private String zineCover;
        private String reviewContent;
        private LocalDateTime createdAt;
    }
}