package com.naskah.demo.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserReadingDashboardResponse {
    // Overview Stats (untuk cards di dashboard)
    private OverviewStats overviewStats;

    // Buku yang sedang dibaca
    private List<BookInProgressItem> booksInProgress;

    // Terakhir dibaca (5 most recent)
    private List<RecentlyReadItem> recentlyRead;

    // Reading patterns & streaks
    private ReadingPatternSummary readingPattern;

    // Annotations summary
    private AnnotationsSummary annotationsSummary;

    // Quick links
    private QuickAccessLinks quickLinks;

    // Recent achievements
    private List<RecentAchievement> recentAchievements;

    @Data
    public static class OverviewStats {
        private Integer totalBooks;              // Total buku yang pernah dibaca
        private Integer booksInProgress;         // Sedang dibaca
        private Integer booksCompleted;          // Selesai dibaca
        private Integer totalReadingTimeHours;   // Total waktu baca (jam)
        private Double averageRating;            // Rating rata-rata
        private Integer currentStreak;           // Hari berturut-turut
        private Integer longestStreak;           // Streak terpanjang
        private Double completionRate;           // Persentase buku yang diselesaikan
    }

    @Data
    public static class BookInProgressItem {
        private Long bookId;
        private String bookTitle;
        private String bookSlug;
        private String coverImageUrl;
        private String authorName;
        private Integer currentChapter;
        private Integer totalChapters;
        private Double progressPercentage;
        private LocalDateTime lastReadAt;
        private Integer remainingMinutes;        // Estimasi waktu tersisa
    }

    @Data
    public static class RecentlyReadItem {
        private Long bookId;
        private String bookTitle;
        private String bookSlug;
        private String coverImageUrl;
        private String authorName;
        private LocalDateTime lastReadAt;
        private String activityType;             // "started", "continued", "completed"
        private Integer chapterNumber;
        private String chapterTitle;
    }

    @Data
    public static class ReadingPatternSummary {
        private String preferredReadingTime;     // "Morning", "Evening", etc.
        private String preferredDay;             // "Weekend", "Weekday", etc.
        private Integer averageSessionMinutes;
        private Integer averageReadingSpeedWpm;
        private Integer currentStreak;
        private Integer longestStreak;
        private String readingPace;              // "Slow", "Moderate", "Fast"
    }

    @Data
    public static class AnnotationsSummary {
        private Integer totalBookmarks;
        private Integer totalHighlights;
        private Integer totalNotes;
        private Integer totalReviews;
        private List<RecentAnnotation> recentAnnotations;
    }

    @Data
    public static class RecentAnnotation {
        private String type;                     // "bookmark", "highlight", "note"
        private String bookTitle;
        private String content;                  // Text preview
        private LocalDateTime createdAt;
        private String bookSlug;
        private Integer chapterNumber;
    }

    @Data
    public static class QuickAccessLinks {
        private Integer pendingBookmarks;
        private Integer unreadHighlights;
        private Integer draftNotes;
        private Integer pendingReviews;
    }

    @Data
    public static class RecentAchievement {
        private String achievementId;
        private String title;
        private String description;
        private String badgeUrl;
        private LocalDateTime unlockedAt;
    }
}