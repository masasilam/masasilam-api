package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.DashboardService;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final HeaderHolder headerHolder;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final ChapterProgressMapper chapterProgressMapper;
    private final ReadingSessionMapper sessionMapper;
    private final ReadingActivityMapper activityMapper;
    private final BookmarkMapper bookmarkMapper;
    private final HighlightMapper highlightMapper;
    private final NoteMapper noteMapper;
    private final BookRatingMapper bookRatingMapper;
    private final BookReviewMapper bookReviewMapper;
    private final UserReadingPatternMapper patternMapper;
    private final GenreMapper genreMapper;

    private static final String SUCCESS = "Success";

    // ═══════════════════════════════════════════════════════════
    // MAIN DASHBOARD
    // ═══════════════════════════════════════════════════════════

    @Override
    public DataResponse<UserReadingDashboardResponse> getUserReadingDashboard() {
        try {
            User user = getCurrentUser();

            UserReadingDashboardResponse response = new UserReadingDashboardResponse();

            // 1. Overview Stats
            response.setOverviewStats(buildOverviewStats(user.getId()));

            // 2. Books In Progress
            response.setBooksInProgress(getBooksInProgress(user.getId(), 5));

            // 3. Recently Read
            response.setRecentlyRead(getRecentlyRead(user.getId(), 5));

            // 4. Reading Pattern
            response.setReadingPattern(buildReadingPatternSummary(user.getId()));

            // 5. Annotations Summary
            response.setAnnotationsSummary(buildAnnotationsSummary(user.getId()));

            // 6. Quick Links
            response.setQuickLinks(buildQuickAccessLinks(user.getId()));

            // 7. Recent Achievements
            response.setRecentAchievements(getRecentAchievements(user.getId(), 3));

            log.info("Dashboard data retrieved for user {}", user.getId());

            return new DataResponse<>(SUCCESS, "Dashboard retrieved successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting dashboard: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LIBRARY
    // ═══════════════════════════════════════════════════════════

    @Override
    public DatatableResponse<BookLibraryItemResponse> getUserLibrary(
            String filter, int page, int limit, String sortBy) {
        try {
            User user = getCurrentUser();

            int offset = (page - 1) * limit;

            // Get all books user has interacted with
            List<Long> bookIds = getInteractedBookIds(user.getId(), filter);

            List<BookLibraryItemResponse> items = bookIds.stream()
                    .skip(offset)
                    .limit(limit)
                    .map(bookId -> buildLibraryItem(user.getId(), bookId))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Sort
            sortLibraryItems(items, sortBy);

            PageDataResponse<BookLibraryItemResponse> pageData =
                    new PageDataResponse<>(page, limit, bookIds.size(), items);

            return new DatatableResponse<>(SUCCESS, "Library retrieved",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting library: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // READING HISTORY
    // ═══════════════════════════════════════════════════════════

    @Override
    public DatatableResponse<ReadingActivityResponse> getReadingHistory(
            int days, int page, int limit) {
        try {
            User user = getCurrentUser();

            LocalDateTime since = LocalDateTime.now().minusDays(days);
            int offset = (page - 1) * limit;

            // Get all activity types
            List<ReadingActivityResponse> activities = new ArrayList<>();

            // 1. Reading sessions
            List<ReadingSession> sessions = sessionMapper.findUserSessionsSince(
                    user.getId(), since, offset, limit);
            activities.addAll(sessions.stream()
                    .map(this::mapSessionToActivity)
                    .collect(Collectors.toList()));

            // 2. Annotations
            activities.addAll(getAnnotationActivities(user.getId(), since));

            // 3. Reviews & Ratings
            activities.addAll(getReviewActivities(user.getId(), since));

            // Sort by timestamp descending
            activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

            // Paginate
            int start = Math.min(offset, activities.size());
            int end = Math.min(start + limit, activities.size());
            List<ReadingActivityResponse> paged = activities.subList(start, end);

            PageDataResponse<ReadingActivityResponse> pageData =
                    new PageDataResponse<>(page, limit, activities.size(), paged);

            return new DatatableResponse<>(SUCCESS, "History retrieved",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting history: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // READING STATISTICS
    // ═══════════════════════════════════════════════════════════

    @Override
    public DataResponse<ReadingStatisticsResponse> getReadingStatistics(int period) {
        try {
            User user = getCurrentUser();

            LocalDateTime since = LocalDateTime.now().minusDays(period);

            ReadingStatisticsResponse response = new ReadingStatisticsResponse();
            response.setPeriod("Last " + period + " days");

            // 1. Daily stats
            response.setDailyStats(buildDailyStats(user.getId(), since, period));

            // 2. Weekly stats
            response.setWeeklyStats(buildWeeklyStats(user.getId(), since));

            // 3. Aggregate stats
            Map<String, Object> aggregates = activityMapper.getUserActivitySummary(
                    user.getId(), since, LocalDateTime.now());

            response.setTotalBooksRead(getIntValue(aggregates.get("books_read")));
            response.setTotalChaptersRead(getIntValue(aggregates.get("chapters_read")));

            // Konversi total_seconds ke total_minutes
            int totalSeconds = getIntValue(aggregates.get("total_seconds"));
            response.setTotalReadingMinutes(totalSeconds / 60);

            response.setAverageReadingSpeedWpm(getDoubleValue(aggregates.get("avg_speed")));

            // 4. Trends - Perlu diperbaiki juga method-method ini
            response.setReadingTimeTrend(calculateTimeTrend(user.getId(), period));
            response.setCompletionTrend(calculateCompletionTrend(user.getId(), period));
            response.setSpeedTrend(calculateSpeedTrend(user.getId(), period));

            // 5. Genre breakdown
            response.setGenreBreakdown(buildGenreBreakdown(user.getId(), since));

            // 6. Peak times
            response.setPeakReadingTimes(buildPeakTimes(user.getId(), since));

            return new DataResponse<>(SUCCESS, "Statistics retrieved",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting statistics: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ANNOTATIONS
    // ═══════════════════════════════════════════════════════════

    @Override
    public DatatableResponse<AnnotationItemResponse> getAllAnnotations(
            String type, int page, int limit, String sortBy) {
        try {
            User user = getCurrentUser();

            int offset = (page - 1) * limit;
            List<AnnotationItemResponse> items = new ArrayList<>();
            int totalCount = 0;

            if (type.equals("all") || type.equals("bookmarks")) {
                List<Bookmark> bookmarks = bookmarkMapper.findByUser(user.getId());
                items.addAll(bookmarks.stream()
                        .map(this::mapBookmarkToAnnotation)
                        .collect(Collectors.toList()));
                totalCount += bookmarks.size();
            }

            if (type.equals("all") || type.equals("highlights")) {
                List<Highlight> highlights = highlightMapper.findByUser(user.getId());
                items.addAll(highlights.stream()
                        .map(this::mapHighlightToAnnotation)
                        .collect(Collectors.toList()));
                totalCount += highlights.size();
            }

            if (type.equals("all") || type.equals("notes")) {
                List<Note> notes = noteMapper.findByUser(user.getId());
                items.addAll(notes.stream()
                        .map(this::mapNoteToAnnotation)
                        .collect(Collectors.toList()));
                totalCount += notes.size();
            }

            // Sort
            sortAnnotations(items, sortBy);

            // Paginate
            int start = Math.min(offset, items.size());
            int end = Math.min(start + limit, items.size());
            List<AnnotationItemResponse> paged = items.subList(start, end);

            PageDataResponse<AnnotationItemResponse> pageData =
                    new PageDataResponse<>(page, limit, totalCount, paged);

            return new DatatableResponse<>(SUCCESS, "Annotations retrieved",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting annotations: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REVIEWS
    // ═══════════════════════════════════════════════════════════

    @Override
    public DatatableResponse<UserReviewItemResponse> getUserReviews(
            int page, int limit) {
        try {
            User user = getCurrentUser();

            int offset = (page - 1) * limit;
            List<BookReview> reviews = bookReviewMapper.findByUser(
                    user.getId(), offset, limit);

            List<UserReviewItemResponse> items = reviews.stream()
                    .map(this::mapToUserReviewItem)
                    .collect(Collectors.toList());

            int totalCount = bookReviewMapper.countByUser(user.getId());

            PageDataResponse<UserReviewItemResponse> pageData =
                    new PageDataResponse<>(page, limit, totalCount, items);

            return new DatatableResponse<>(SUCCESS, "Reviews retrieved",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting reviews: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // READING GOALS (Placeholder - requires new table)
    // ═══════════════════════════════════════════════════════════

    @Override
    public DataResponse<ReadingGoalsResponse> getReadingGoals() {
        try {
            User user = getCurrentUser();

            ReadingGoalsResponse response = new ReadingGoalsResponse();
            response.setActiveGoals(new ArrayList<>());
            response.setCompletedGoals(new ArrayList<>());

            ReadingGoalsResponse.GoalsSummary summary =
                    new ReadingGoalsResponse.GoalsSummary();
            summary.setTotalGoals(0);
            summary.setActiveGoals(0);
            summary.setCompletedGoals(0);
            summary.setOverallProgress(0.0);
            summary.setOnTrack(true);

            response.setSummary(summary);

            // TODO: Implement when reading_goals table is created

            return new DataResponse<>(SUCCESS, "Goals retrieved",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting goals: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // RECOMMENDATIONS
    // ═══════════════════════════════════════════════════════════

    @Override
    public DataResponse<List<BookRecommendationResponse>> getPersonalizedRecommendations(
            int limit) {
        try {
            User user = getCurrentUser();

            // Get user's favorite genres based on reading history
            List<String> favoriteGenres = getUserFavoriteGenres(user.getId(), 3);

            // Get highly rated books in those genres that user hasn't read
            List<BookRecommendationResponse> recommendations =
                    bookMapper.getRecommendations(user.getId(), favoriteGenres, limit);

            // Calculate match scores
            recommendations.forEach(rec -> {
                rec.setMatchScore(calculateMatchScore(user.getId(), rec));
                rec.setMatchingFactors(identifyMatchingFactors(user.getId(), rec));
            });

            return new DataResponse<>(SUCCESS, "Recommendations retrieved",
                    HttpStatus.OK.value(), recommendations);

        } catch (Exception e) {
            log.error("Error getting recommendations: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // QUICK STATS
    // ═══════════════════════════════════════════════════════════

    @Override
    public DataResponse<QuickStatsResponse> getQuickStats() {
        try {
            User user = getCurrentUser();

            QuickStatsResponse response = new QuickStatsResponse();

            // Get basic counts (fast queries)
            List<Long> bookIds = getInteractedBookIds(user.getId(), "all");
            response.setTotalBooks(bookIds.size());

            // Total reading time
            Integer totalMinutes = activityMapper.getUserTotalReadingMinutes(user.getId());
            response.setReadingTime(formatReadingTime(totalMinutes));

            // Completed books
            Integer completed = chapterProgressMapper.countCompletedBooks(user.getId());
            response.setCompletedBooks(completed);

            // Average rating
            Double avgRating = bookRatingMapper.getUserAverageRating(user.getId());
            response.setAverageRating(avgRating != null ? avgRating : 0.0);

            // Current streak
            Integer streak = calculateCurrentStreak(user.getId());
            response.setCurrentStreak(streak);

            // Has activity today
            LocalDateTime todayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
            Boolean hasActivity = activityMapper.hasActivitySince(
                    user.getId(), todayStart);
            response.setHasActivityToday(hasActivity);

            return new DataResponse<>(SUCCESS, "Quick stats retrieved",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting quick stats: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // READING CALENDAR
    // ═══════════════════════════════════════════════════════════

    @Override
    public DataResponse<ReadingCalendarResponse> getReadingCalendar(
            Integer year, Integer month) {
        try {
            User user = getCurrentUser();

            // Default to current month
            if (year == null) year = LocalDate.now().getYear();
            if (month == null) month = LocalDate.now().getMonthValue();

            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

            ReadingCalendarResponse response = new ReadingCalendarResponse();
            response.setYear(year);
            response.setMonth(month);

            // Build calendar days
            List<ReadingCalendarResponse.CalendarDay> days = new ArrayList<>();

            for (LocalDate date = startDate; !date.isAfter(endDate);
                 date = date.plusDays(1)) {

                ReadingCalendarResponse.CalendarDay day = buildCalendarDay(
                        user.getId(), date);
                days.add(day);
            }

            response.setDays(days);

            // Calculate stats
            ReadingCalendarResponse.CalendarStats stats =
                    new ReadingCalendarResponse.CalendarStats();

            long daysWithActivity = days.stream()
                    .filter(ReadingCalendarResponse.CalendarDay::getHasActivity)
                    .count();

            stats.setDaysWithActivity((int) daysWithActivity);
            stats.setTotalDays(days.size());
            stats.setActivityPercentage(
                    (daysWithActivity * 100.0) / days.size());
            stats.setTotalMinutes(days.stream()
                    .mapToInt(ReadingCalendarResponse.CalendarDay::getMinutesRead)
                    .sum());

            // Calculate longest streak in this period
            stats.setLongestStreakInPeriod(
                    calculateStreakInPeriod(days));

            response.setStats(stats);

            return new DataResponse<>(SUCCESS, "Calendar retrieved",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting calendar: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ACHIEVEMENTS
    // ═══════════════════════════════════════════════════════════

    @Override
    public DataResponse<List<AchievementResponse>> getUserAchievements() {
        try {
            User user = getCurrentUser();

            List<AchievementResponse> achievements = new ArrayList<>();

            // Calculate achievements dynamically
            achievements.addAll(calculateReadingAchievements(user.getId()));
            achievements.addAll(calculateStreakAchievements(user.getId()));
            achievements.addAll(calculateSocialAchievements(user.getId()));

            // Sort by unlocked status and date
            achievements.sort((a, b) -> {
                if (a.getIsUnlocked() != b.getIsUnlocked()) {
                    return a.getIsUnlocked() ? -1 : 1;
                }
                if (a.getUnlockedAt() != null && b.getUnlockedAt() != null) {
                    return b.getUnlockedAt().compareTo(a.getUnlockedAt());
                }
                return 0;
            });

            return new DataResponse<>(SUCCESS, "Achievements retrieved",
                    HttpStatus.OK.value(), achievements);

        } catch (Exception e) {
            log.error("Error getting achievements: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // EXPORT
    // ═══════════════════════════════════════════════════════════

    @Override
    public DataResponse<ExportJobResponse> exportUserReadingData(String format) {
        try {
            User user = getCurrentUser();

            ExportJobResponse response = new ExportJobResponse();
            response.setExportId(System.currentTimeMillis());
            response.setFormat(format);
            response.setStatus("pending");
            response.setRequestedAt(LocalDateTime.now());
            response.setExpiresAt(LocalDateTime.now().plusDays(7));

            // TODO: Implement actual export job
            // This would typically:
            // 1. Create export record in database
            // 2. Queue async job
            // 3. Return job ID for status polling

            log.info("Export requested for user {} in format {}",
                    user.getId(), format);

            return new DataResponse<>(SUCCESS, "Export job created",
                    HttpStatus.ACCEPTED.value(), response);

        } catch (Exception e) {
            log.error("Error creating export: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS - Building Response Components
    // ═══════════════════════════════════════════════════════════

    private UserReadingDashboardResponse.OverviewStats buildOverviewStats(Long userId) {
        UserReadingDashboardResponse.OverviewStats stats =
                new UserReadingDashboardResponse.OverviewStats();

        // Total books (unique books user has interacted with)
        List<Long> bookIds = getInteractedBookIds(userId, "all");
        stats.setTotalBooks(bookIds.size());

        // Books in progress
        int inProgress = (int) bookIds.stream()
                .filter(bookId -> isBookInProgress(userId, bookId))
                .count();
        stats.setBooksInProgress(inProgress);

        // Books completed
        Integer completed = chapterProgressMapper.countCompletedBooks(userId);
        stats.setBooksCompleted(completed != null ? completed : 0);

        // Total reading time
        Integer totalMinutes = activityMapper.getUserTotalReadingMinutes(userId);
        int hours = totalMinutes != null ? totalMinutes / 60 : 0;
        stats.setTotalReadingTimeHours(hours);

        // Average rating
        Double avgRating = bookRatingMapper.getUserAverageRating(userId);
        stats.setAverageRating(avgRating != null ? avgRating : 0.0);

        // Streaks
        Integer currentStreak = calculateCurrentStreak(userId);
        Integer longestStreak = calculateLongestStreak(userId);
        stats.setCurrentStreak(currentStreak);
        stats.setLongestStreak(longestStreak);

        // Completion rate
        if (stats.getTotalBooks() > 0) {
            double rate = (stats.getBooksCompleted() * 100.0) / stats.getTotalBooks();
            stats.setCompletionRate(rate);
        } else {
            stats.setCompletionRate(0.0);
        }

        return stats;
    }

    private List<UserReadingDashboardResponse.BookInProgressItem> getBooksInProgress(
            Long userId, int limit) {

        List<Long> bookIds = getInteractedBookIds(userId, "reading");

        return bookIds.stream()
                .limit(limit)
                .map(bookId -> {
                    Book book = bookMapper.findById(bookId);
                    if (book == null) return null;

                    UserReadingDashboardResponse.BookInProgressItem item =
                            new UserReadingDashboardResponse.BookInProgressItem();

                    item.setBookId(book.getId());
                    item.setBookTitle(book.getTitle());
                    item.setBookSlug(book.getSlug());
                    item.setCoverImageUrl(book.getCoverImageUrl());

                    // Get author
                    List<Author> authors = bookMapper.getBookAuthors(book.getId());
                    if (!authors.isEmpty()) {
                        item.setAuthorName(authors.get(0).getName());
                    }

                    // Get progress
                    List<ChapterProgress> progress =
                            chapterProgressMapper.findAllByUserAndBook(userId, book.getId());

                    int completed = (int) progress.stream()
                            .filter(ChapterProgress::getIsCompleted)
                            .count();

                    item.setCurrentChapter(completed + 1);
                    item.setTotalChapters(book.getTotalPages());

                    double percentage = (completed * 100.0) / book.getTotalPages();
                    item.setProgressPercentage(percentage);

                    // Last read
                    LocalDateTime lastRead = progress.stream()
                            .map(ChapterProgress::getLastReadAt)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    item.setLastReadAt(lastRead);

                    // Remaining time estimate
                    int remainingChapters = book.getTotalPages() - completed;
                    int avgTimePerChapter = book.getEstimatedReadTime() / book.getTotalPages();
                    item.setRemainingMinutes(remainingChapters * avgTimePerChapter);

                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<UserReadingDashboardResponse.RecentlyReadItem> getRecentlyRead(
            Long userId, int limit) {

        // Get recent sessions
        List<ReadingSession> sessions = sessionMapper.findUserRecentSessions(
                userId, 0, limit);

        return sessions.stream()
                .map(session -> {
                    Book book = bookMapper.findById(session.getBookId());
                    if (book == null) return null;

                    UserReadingDashboardResponse.RecentlyReadItem item =
                            new UserReadingDashboardResponse.RecentlyReadItem();

                    item.setBookId(book.getId());
                    item.setBookTitle(book.getTitle());
                    item.setBookSlug(book.getSlug());
                    item.setCoverImageUrl(book.getCoverImageUrl());

                    // Get author
                    List<Author> authors = bookMapper.getBookAuthors(book.getId());
                    if (!authors.isEmpty()) {
                        item.setAuthorName(authors.get(0).getName());
                    }

                    item.setLastReadAt(session.getEndedAt() != null ?
                            session.getEndedAt() : session.getStartedAt());

                    // Determine activity type
                    if (session.getStartChapter().equals(session.getEndChapter())) {
                        item.setActivityType("continued");
                    } else if (isBookCompleted(userId, book.getId())) {
                        item.setActivityType("completed");
                    } else {
                        item.setActivityType("started");
                    }

                    item.setChapterNumber(session.getEndChapter());

                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private UserReadingDashboardResponse.ReadingPatternSummary buildReadingPatternSummary(
            Long userId) {

        UserReadingDashboardResponse.ReadingPatternSummary summary =
                new UserReadingDashboardResponse.ReadingPatternSummary();

        // Get aggregated pattern from all books
        List<UserReadingPattern> patterns = patternMapper.findAllUserPatterns(userId);

        if (!patterns.isEmpty()) {
            // Calculate averages
            Integer avgHour = (int) patterns.stream()
                    .mapToInt(p -> p.getPreferredReadingHour() != null ?
                            p.getPreferredReadingHour() : 12)
                    .average()
                    .orElse(12);

            summary.setPreferredReadingTime(getTimeOfDayLabel(avgHour));
            summary.setPreferredDay("Weekday"); // Simplified

            Integer avgSession = (int) patterns.stream()
                    .mapToInt(p -> p.getAverageSessionDurationMinutes() != null ?
                            p.getAverageSessionDurationMinutes() : 0)
                    .average()
                    .orElse(0);
            summary.setAverageSessionMinutes(avgSession);

            Integer avgSpeed = (int) patterns.stream()
                    .mapToInt(p -> p.getAverageReadingSpeedWpm() != null ?
                            p.getAverageReadingSpeedWpm() : 200)
                    .average()
                    .orElse(200);
            summary.setAverageReadingSpeedWpm(avgSpeed);

            Double avgCompletionSpeed = patterns.stream()
                    .mapToDouble(p -> p.getCompletionSpeedChaptersPerDay() != null ?
                            p.getCompletionSpeedChaptersPerDay() : 1.0)
                    .average()
                    .orElse(1.0);
            summary.setReadingPace(getPaceLabel(avgCompletionSpeed));
        } else {
            // Defaults
            summary.setPreferredReadingTime("Evening");
            summary.setPreferredDay("Weekday");
            summary.setAverageSessionMinutes(30);
            summary.setAverageReadingSpeedWpm(200);
            summary.setReadingPace("Moderate");
        }

        // Streaks
        summary.setCurrentStreak(calculateCurrentStreak(userId));
        summary.setLongestStreak(calculateLongestStreak(userId));

        return summary;
    }

// ═══════════════════════════════════════════════════════════
// CONTINUATION OF ReadingDashboardServiceImpl
// ═══════════════════════════════════════════════════════════

    // ... continued from buildAnnotationsSummary method

    private UserReadingDashboardResponse.AnnotationsSummary buildAnnotationsSummary(
            Long userId) {

        UserReadingDashboardResponse.AnnotationsSummary summary =
                new UserReadingDashboardResponse.AnnotationsSummary();

        // Counts
        summary.setTotalBookmarks(bookmarkMapper.countByUser(userId));
        summary.setTotalHighlights(highlightMapper.countByUser(userId));
        summary.setTotalNotes(noteMapper.countByUser(userId));
        summary.setTotalReviews(bookReviewMapper.countByUser(userId));

        // Recent annotations (mixed)
        List<UserReadingDashboardResponse.RecentAnnotation> recent = new ArrayList<>();

        // Get 2 most recent from each type
        List<Bookmark> recentBookmarks = bookmarkMapper.findRecentByUser(userId, 2);
        recentBookmarks.forEach(b -> recent.add(mapBookmarkToRecentAnnotation(b)));

        List<Highlight> recentHighlights = highlightMapper.findRecentByUser(userId, 2);
        recentHighlights.forEach(h -> recent.add(mapHighlightToRecentAnnotation(h)));

        List<Note> recentNotes = noteMapper.findRecentByUser(userId, 2);
        recentNotes.forEach(n -> recent.add(mapNoteToRecentAnnotation(n)));

        // Sort by date and take top 5
        recent.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        summary.setRecentAnnotations(recent.stream().limit(5).collect(Collectors.toList()));

        return summary;
    }

    private UserReadingDashboardResponse.QuickAccessLinks buildQuickAccessLinks(Long userId) {
        UserReadingDashboardResponse.QuickAccessLinks links =
                new UserReadingDashboardResponse.QuickAccessLinks();

        // Count pending/unread items
        links.setPendingBookmarks(bookmarkMapper.countByUser(userId));
        links.setUnreadHighlights(highlightMapper.countByUser(userId));
        links.setDraftNotes(noteMapper.countDraftsByUser(userId));

        // Count books without reviews
        int totalRead = chapterProgressMapper.countCompletedBooks(userId);
        int reviewCount = bookReviewMapper.countByUser(userId);
        links.setPendingReviews(Math.max(0, totalRead - reviewCount));

        return links;
    }

    private List<UserReadingDashboardResponse.RecentAchievement> getRecentAchievements(
            Long userId, int limit) {

        List<UserReadingDashboardResponse.RecentAchievement> achievements =
                new ArrayList<>();

        // Calculate recent achievements
        List<AchievementResponse> all = new ArrayList<>();
        all.addAll(calculateReadingAchievements(userId));
        all.addAll(calculateStreakAchievements(userId));

        // Filter unlocked and sort by date
        return all.stream()
                .filter(AchievementResponse::getIsUnlocked)
                .sorted((a, b) -> b.getUnlockedAt().compareTo(a.getUnlockedAt()))
                .limit(limit)
                .map(this::mapToRecentAchievement)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // LIBRARY HELPERS
    // ═══════════════════════════════════════════════════════════

    private List<Long> getInteractedBookIds(Long userId, String filter) {
        Set<Long> bookIds = new HashSet<>();

        // Books with reading progress
        List<ChapterProgress> progress = chapterProgressMapper.findAllByUser(userId);
        progress.forEach(p -> bookIds.add(p.getBookId()));

        // Books with bookmarks
        List<Bookmark> bookmarks = bookmarkMapper.findByUser(userId);
        bookmarks.forEach(b -> bookIds.add(b.getBookId()));

        // Books with highlights
        List<Highlight> highlights = highlightMapper.findByUser(userId);
        highlights.forEach(h -> bookIds.add(h.getBookId()));

        // Books with notes
        List<Note> notes = noteMapper.findByUser(userId);
        notes.forEach(n -> bookIds.add(n.getBookId()));

        // Books with ratings/reviews
        List<BookRating> ratings = bookRatingMapper.findByUser(userId);
        ratings.forEach(r -> bookIds.add(r.getBookId()));

        // Apply filter
        List<Long> filtered = new ArrayList<>(bookIds);

        if ("reading".equals(filter)) {
            filtered.removeIf(bookId -> !isBookInProgress(userId, bookId));
        } else if ("completed".equals(filter)) {
            filtered.removeIf(bookId -> !isBookCompleted(userId, bookId));
        } else if ("bookmarked".equals(filter)) {
            List<Long> bookmarkedIds = bookmarks.stream()
                    .map(Bookmark::getBookId)
                    .distinct()
                    .collect(Collectors.toList());
            filtered.retainAll(bookmarkedIds);
        }

        return filtered;
    }

    private BookLibraryItemResponse buildLibraryItem(Long userId, Long bookId) {
        Book book = bookMapper.findById(bookId);
        if (book == null) return null;

        BookLibraryItemResponse item = new BookLibraryItemResponse();

        // Info dasar buku
        item.setBookId(book.getId());
        item.setBookTitle(book.getTitle());
        item.setBookSlug(book.getSlug());
        item.setCoverImageUrl(book.getCoverImageUrl());

        // Author
        List<Author> authors = bookMapper.getBookAuthors(book.getId());
        if (!authors.isEmpty()) {
            item.setAuthorName(authors.get(0).getName());
        }

        // Genre
        List<Genre> genres = bookMapper.getBookGenres(book.getId());
        if (!genres.isEmpty()) {
            item.setGenre(genres.get(0).getName());
        }

        // Progress membaca
        List<ChapterProgress> progress = chapterProgressMapper.findAllByUserAndBook(userId, book.getId());
        long completed = progress.stream()
                .filter(p -> p.getIsCompleted() != null && p.getIsCompleted())
                .count();

        item.setCurrentChapter((int) completed);
        item.setTotalChapters(book.getTotalPages());

        // Hitung persentase progress
        if (book.getTotalPages() > 0) {
            item.setProgressPercentage((completed * 100.0) / book.getTotalPages());
        } else {
            item.setProgressPercentage(0.0);
        }

        // Status membaca
        if (completed == 0) {
            item.setReadingStatus("not_started");
        } else if (completed >= book.getTotalPages()) {
            item.setReadingStatus("completed");
        } else {
            item.setReadingStatus("reading");
        }

        // Count engagement
        item.setBookmarkCount(bookmarkMapper.countByUserAndBook(userId, bookId));
        item.setHighlightCount(highlightMapper.countByUserAndBook(userId, bookId));
        item.setNoteCount(noteMapper.countByUserAndBook(userId, bookId));

        // Rating & review
        BookRating rating = bookRatingMapper.findByUserAndBook(userId, bookId);
        item.setMyRating(rating != null ? rating.getRating() : null);

        BookReview review = bookReviewMapper.findByUserAndBook(userId, bookId);
        item.setHasReview(review != null);

        // Timestamps - PERBAIKAN DISINI
        if (!progress.isEmpty()) {
            // First read - gunakan createdAt atau lastReadAt yang paling awal
            item.setFirstReadAt(progress.stream()
                    .map(p -> {
                        // Prioritas: lastReadAt, kemudian createdAt
                        if (p.getLastReadAt() != null) {
                            return p.getLastReadAt();
                        }
                        return p.getCreatedAt();
                    })
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null));

            // Last read - cari lastReadAt yang paling baru
            item.setLastReadAt(progress.stream()
                    .map(ChapterProgress::getLastReadAt)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null));

            // Completed at - cari waktu ketika chapter selesai (lastReadAt dari chapter yang completed)
            if (completed >= book.getTotalPages()) {
                item.setCompletedAt(progress.stream()
                        .filter(p -> p.getIsCompleted() != null && p.getIsCompleted())
                        .map(ChapterProgress::getLastReadAt)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null));
            }
        }

        // Total waktu membaca (dalam menit)
        List<ReadingSession> sessions = sessionMapper.findByUserAndBook(userId, bookId);
        int totalMinutes = sessions.stream()
                .mapToInt(s -> {
                    if (s.getTotalDurationSeconds() != null) {
                        return s.getTotalDurationSeconds() / 60;  // Konversi detik ke menit
                    }
                    return 0;
                })
                .sum();
        item.setTotalReadingTimeMinutes(totalMinutes);

        // Hitung total waktu membaca dari chapter progress (alternatif)
        int totalReadingTimeFromProgress = progress.stream()
                .mapToInt(p -> p.getReadingTimeSeconds() != null ?
                        p.getReadingTimeSeconds() / 60 : 0)
                .sum();

        // Gunakan yang terbesar antara session time dan progress time
        item.setTotalReadingTimeMinutes(Math.max(totalMinutes, totalReadingTimeFromProgress));

        // Estimated remaining time
        int remainingChapters = book.getTotalPages() - (int) completed;

        // Hitung rata-rata waktu per chapter dari data aktual
        int avgTimePerChapter = 0;
        if (completed > 0 && totalReadingTimeFromProgress > 0) {
            // Gunakan rata-rata waktu aktual per chapter
            avgTimePerChapter = totalReadingTimeFromProgress / (int) completed;
        } else if (book.getTotalPages() > 0 && book.getEstimatedReadTime() > 0) {
            // Fallback ke estimated time dari buku
            avgTimePerChapter = book.getEstimatedReadTime() / book.getTotalPages();
        }

        item.setEstimatedTimeRemaining(remainingChapters * avgTimePerChapter);

        return item;
    }

    private void sortLibraryItems(List<BookLibraryItemResponse> items, String sortBy) {
        switch (sortBy) {
            case "last_read":
                items.sort((a, b) -> {
                    if (a.getLastReadAt() == null) return 1;
                    if (b.getLastReadAt() == null) return -1;
                    return b.getLastReadAt().compareTo(a.getLastReadAt());
                });
                break;
            case "progress":
                items.sort((a, b) -> Double.compare(
                        b.getProgressPercentage(), a.getProgressPercentage()));
                break;
            case "title":
                items.sort(Comparator.comparing(BookLibraryItemResponse::getBookTitle));
                break;
            case "rating":
                items.sort((a, b) -> {
                    if (a.getMyRating() == null) return 1;
                    if (b.getMyRating() == null) return -1;
                    return Double.compare(b.getMyRating(), a.getMyRating());
                });
                break;
            default:
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // READING HISTORY HELPERS
    // ═══════════════════════════════════════════════════════════

    private ReadingActivityResponse mapSessionToActivity(ReadingSession session) {
        ReadingActivityResponse activity = new ReadingActivityResponse();

        activity.setActivityId(session.getId());
        activity.setActivityType("reading_session");
        activity.setTimestamp(session.getStartedAt());
        activity.setBookId(session.getBookId());

        Book book = bookMapper.findById(session.getBookId());
        if (book != null) {
            activity.setBookTitle(book.getTitle());
            activity.setBookSlug(book.getSlug());
            activity.setBookCover(book.getCoverImageUrl());
        }

        activity.setChapterNumber(session.getEndChapter());

        // Hitung durasi
        int minutes = session.getTotalDurationSeconds() != null ?
                session.getTotalDurationSeconds() / 60 : 0;

        activity.setDescription(String.format(
                "Membaca selama %d menit",
                minutes));

        return activity;
    }

    private List<ReadingActivityResponse> getAnnotationActivities(
            Long userId, LocalDateTime since) {

        List<ReadingActivityResponse> activities = new ArrayList<>();

        // Bookmarks
        List<Bookmark> bookmarks = bookmarkMapper.findByUserSince(userId, since);
        bookmarks.forEach(b -> activities.add(mapBookmarkToActivity(b)));

        // Highlights
        List<Highlight> highlights = highlightMapper.findByUserSince(userId, since);
        highlights.forEach(h -> activities.add(mapHighlightToActivity(h)));

        // Notes
        List<Note> notes = noteMapper.findByUserSince(userId, since);
        notes.forEach(n -> activities.add(mapNoteToActivity(n)));

        return activities;
    }

    private List<ReadingActivityResponse> getReviewActivities(
            Long userId, LocalDateTime since) {

        List<ReadingActivityResponse> activities = new ArrayList<>();

        // Ratings
        List<BookRating> ratings = bookRatingMapper.findByUserSince(userId, since);
        ratings.forEach(r -> activities.add(mapRatingToActivity(r)));

        // Reviews
        List<BookReview> reviews = bookReviewMapper.findByUserSince(userId, since);
        reviews.forEach(r -> activities.add(mapReviewToActivity(r)));

        return activities;
    }

    private ReadingActivityResponse mapBookmarkToActivity(Bookmark bookmark) {
        ReadingActivityResponse activity = new ReadingActivityResponse();
        activity.setActivityId(bookmark.getId());
        activity.setActivityType("add_bookmark");
        activity.setTimestamp(bookmark.getCreatedAt());
        activity.setBookId(bookmark.getBookId());
        activity.setChapterNumber(bookmark.getChapterNumber());
        activity.setDescription("Added a bookmark");

        Book book = bookMapper.findById(bookmark.getBookId());
        if (book != null) {
            activity.setBookTitle(book.getTitle());
            activity.setBookSlug(book.getSlug());
            activity.setBookCover(book.getCoverImageUrl());
        }

        return activity;
    }

    private ReadingActivityResponse mapHighlightToActivity(Highlight highlight) {
        ReadingActivityResponse activity = new ReadingActivityResponse();
        activity.setActivityId(highlight.getId());
        activity.setActivityType("add_highlight");
        activity.setTimestamp(highlight.getCreatedAt());
        activity.setBookId(highlight.getBookId());
        activity.setChapterNumber(highlight.getChapterNumber());
        activity.setDescription("Highlighted text");

        Book book = bookMapper.findById(highlight.getBookId());
        if (book != null) {
            activity.setBookTitle(book.getTitle());
            activity.setBookSlug(book.getSlug());
            activity.setBookCover(book.getCoverImageUrl());
        }

        return activity;
    }

    private ReadingActivityResponse mapNoteToActivity(Note note) {
        ReadingActivityResponse activity = new ReadingActivityResponse();
        activity.setActivityId(note.getId());
        activity.setActivityType("add_note");
        activity.setTimestamp(note.getCreatedAt());
        activity.setBookId(note.getBookId());
        activity.setChapterNumber(note.getChapterNumber());
        activity.setDescription("Added a note");

        Book book = bookMapper.findById(note.getBookId());
        if (book != null) {
            activity.setBookTitle(book.getTitle());
            activity.setBookSlug(book.getSlug());
            activity.setBookCover(book.getCoverImageUrl());
        }

        return activity;
    }

    private ReadingActivityResponse mapRatingToActivity(BookRating rating) {
        ReadingActivityResponse activity = new ReadingActivityResponse();
        activity.setActivityId(rating.getId());
        activity.setActivityType("add_rating");
        activity.setTimestamp(rating.getCreatedAt());
        activity.setBookId(rating.getBookId());
        activity.setDescription(String.format("Rated %.1f stars", rating.getRating()));

        Book book = bookMapper.findById(rating.getBookId());
        if (book != null) {
            activity.setBookTitle(book.getTitle());
            activity.setBookSlug(book.getSlug());
            activity.setBookCover(book.getCoverImageUrl());
        }

        return activity;
    }

    private ReadingActivityResponse mapReviewToActivity(BookReview review) {
        ReadingActivityResponse activity = new ReadingActivityResponse();
        activity.setActivityId(review.getId());
        activity.setActivityType("add_review");
        activity.setTimestamp(review.getCreatedAt());
        activity.setBookId(review.getBookId());
        activity.setDescription("Wrote a review");

        Book book = bookMapper.findById(review.getBookId());
        if (book != null) {
            activity.setBookTitle(book.getTitle());
            activity.setBookSlug(book.getSlug());
            activity.setBookCover(book.getCoverImageUrl());
        }

        return activity;
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS HELPERS
    // ═══════════════════════════════════════════════════════════

    private List<ReadingStatisticsResponse.DailyReadingData> buildDailyStats(
            Long userId, LocalDateTime since, int days) {

        List<ReadingStatisticsResponse.DailyReadingData> dailyStats = new ArrayList<>();

        LocalDate startDate = since.toLocalDate();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            if (date.isAfter(today)) break;

            ReadingStatisticsResponse.DailyReadingData data =
                    new ReadingStatisticsResponse.DailyReadingData();

            data.setDate(date.toString());

            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            // Ambil sesi untuk hari ini
            List<ReadingSession> sessions = sessionMapper.findUserSessionsBetween(
                    userId, dayStart, dayEnd);

            // Hitung total menit membaca
            int totalMinutes = sessions.stream()
                    .mapToInt(s -> {
                        if (s.getTotalDurationSeconds() != null) {
                            // Konversi detik ke menit
                            return s.getTotalDurationSeconds() / 60;
                        }
                        return 0;
                    })
                    .sum();

            data.setMinutesRead(totalMinutes);
            data.setSessionsCount(sessions.size());
            data.setHadActivity(totalMinutes > 0);

            // Hitung bab yang selesai dibaca
            List<ChapterProgress> completed = chapterProgressMapper
                    .findCompletedBetween(userId, dayStart, dayEnd);
            data.setChaptersCompleted(completed.size());

            dailyStats.add(data);
        }

        return dailyStats;
    }

    private List<ReadingStatisticsResponse.WeeklyReadingData> buildWeeklyStats(
            Long userId, LocalDateTime since) {

        List<ReadingStatisticsResponse.WeeklyReadingData> weeklyStats = new ArrayList<>();

        LocalDate startDate = since.toLocalDate();
        LocalDate today = LocalDate.now();

        LocalDate weekStart = startDate.with(DayOfWeek.MONDAY);

        while (!weekStart.isAfter(today)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            ReadingStatisticsResponse.WeeklyReadingData data =
                    new ReadingStatisticsResponse.WeeklyReadingData();

            data.setWeekStart(weekStart.toString());
            data.setWeekEnd(weekEnd.toString());

            // Ambil sesi untuk minggu ini
            List<ReadingSession> sessions = sessionMapper.findUserSessionsBetween(
                    userId,
                    weekStart.atStartOfDay(),
                    weekEnd.plusDays(1).atStartOfDay());

            // Hitung total menit membaca
            int totalMinutes = sessions.stream()
                    .mapToInt(s -> {
                        if (s.getTotalDurationSeconds() != null) {
                            // Konversi detik ke menit
                            return s.getTotalDurationSeconds() / 60;
                        }
                        return 0;
                    })
                    .sum();

            data.setMinutesRead(totalMinutes);

            // Hitung rata-rata harian (dengan pengecekan untuk menghindari division by zero)
            int daysInWeek = 7;
            data.setAverageDailyMinutes(totalMinutes / daysInWeek);

            // Hitung buku yang selesai dibaca minggu ini
            int booksCompleted = chapterProgressMapper.countBooksCompletedBetween(
                    userId,
                    weekStart.atStartOfDay(),
                    weekEnd.plusDays(1).atStartOfDay());
            data.setBooksCompleted(booksCompleted);

            weeklyStats.add(data);
            weekStart = weekStart.plusWeeks(1);
        }

        return weeklyStats;
    }

    private ReadingStatisticsResponse.TrendData calculateTimeTrend(
            Long userId, int period) {

        ReadingStatisticsResponse.TrendData trend =
                new ReadingStatisticsResponse.TrendData();

        LocalDateTime halfPoint = LocalDateTime.now().minusDays(period / 2);
        LocalDateTime start = LocalDateTime.now().minusDays(period);

        Integer firstHalfMinutes = activityMapper.getTotalMinutesBetween(
                userId, start, halfPoint);
        Integer secondHalfMinutes = activityMapper.getTotalMinutesBetween(
                userId, halfPoint, LocalDateTime.now());

        if (firstHalfMinutes > 0) {
            double change = ((secondHalfMinutes - firstHalfMinutes) * 100.0)
                    / firstHalfMinutes;
            trend.setChangePercentage(change);

            if (change > 10) {
                trend.setDirection("up");
                trend.setInterpretation("Reading time is increasing");
            } else if (change < -10) {
                trend.setDirection("down");
                trend.setInterpretation("Reading time is decreasing");
            } else {
                trend.setDirection("stable");
                trend.setInterpretation("Reading time is stable");
            }
        } else {
            trend.setDirection("stable");
            trend.setChangePercentage(0.0);
            trend.setInterpretation("Not enough data");
        }

        return trend;
    }

    private ReadingStatisticsResponse.TrendData calculateCompletionTrend(
            Long userId, int period) {

        ReadingStatisticsResponse.TrendData trend =
                new ReadingStatisticsResponse.TrendData();

        LocalDateTime halfPoint = LocalDateTime.now().minusDays(period / 2);
        LocalDateTime start = LocalDateTime.now().minusDays(period);

        int firstHalf = chapterProgressMapper.countBooksCompletedBetween(
                userId, start, halfPoint);
        int secondHalf = chapterProgressMapper.countBooksCompletedBetween(
                userId, halfPoint, LocalDateTime.now());

        if (firstHalf > 0) {
            double change = ((secondHalf - firstHalf) * 100.0) / firstHalf;
            trend.setChangePercentage(change);

            if (change > 0) {
                trend.setDirection("up");
                trend.setInterpretation("Completing more books");
            } else if (change < 0) {
                trend.setDirection("down");
                trend.setInterpretation("Completing fewer books");
            } else {
                trend.setDirection("stable");
                trend.setInterpretation("Completion rate is stable");
            }
        } else {
            trend.setDirection("stable");
            trend.setChangePercentage(0.0);
            trend.setInterpretation("Not enough data");
        }

        return trend;
    }

    private ReadingStatisticsResponse.TrendData calculateSpeedTrend(Long userId, int period) {
        ReadingStatisticsResponse.TrendData trend = new ReadingStatisticsResponse.TrendData();

        LocalDateTime halfPoint = LocalDateTime.now().minusDays(period / 2);
        LocalDateTime start = LocalDateTime.now().minusDays(period);

        // Get speed for first half
        Map<String, Object> firstHalf = activityMapper.getUserActivitySummary(
                userId, start, halfPoint);
        double firstHalfSpeed = getDoubleValue(firstHalf.get("avg_speed"));

        // Get speed for second half
        Map<String, Object> secondHalf = activityMapper.getUserActivitySummary(
                userId, halfPoint, LocalDateTime.now());
        double secondHalfSpeed = getDoubleValue(secondHalf.get("avg_speed"));

        if (firstHalfSpeed > 0) {
            double change = ((secondHalfSpeed - firstHalfSpeed) * 100.0) / firstHalfSpeed;
            trend.setChangePercentage(change);

            if (change > 10) {
                trend.setDirection("up");
                trend.setInterpretation("Reading speed is increasing");
            } else if (change < -10) {
                trend.setDirection("down");
                trend.setInterpretation("Reading speed is decreasing");
            } else {
                trend.setDirection("stable");
                trend.setInterpretation("Reading speed is stable");
            }
        } else {
            trend.setDirection("stable");
            trend.setChangePercentage(0.0);
            trend.setInterpretation("Not enough data");
        }

        return trend;
    }

    private List<ReadingStatisticsResponse.GenreStats> buildGenreBreakdown(
            Long userId, LocalDateTime since) {

        Map<String, ReadingStatisticsResponse.GenreStats> genreMap = new HashMap<>();

        // Ambil semua sesi membaca sejak tanggal tertentu
        List<ReadingSession> sessions = sessionMapper.findUserSessionsSince(
                userId, since, 0, Integer.MAX_VALUE);

        // Set untuk melacak buku yang sudah dihitung (hindari duplikasi)
        Set<Long> countedBooks = new HashSet<>();

        for (ReadingSession session : sessions) {
            Book book = bookMapper.findById(session.getBookId());
            if (book == null) continue;

            // Hindari menghitung buku yang sama berulang kali
            if (!countedBooks.contains(book.getId())) {
                countedBooks.add(book.getId());

                List<Genre> genres = bookMapper.getBookGenres(book.getId());
                if (genres.isEmpty()) continue;

                // Ambil genre pertama (atau bisa diubah untuk menangani multiple genres)
                String genreName = genres.get(0).getName();

                ReadingStatisticsResponse.GenreStats stats = genreMap.computeIfAbsent(
                        genreName, k -> {
                            ReadingStatisticsResponse.GenreStats s =
                                    new ReadingStatisticsResponse.GenreStats();
                            s.setGenreName(genreName);
                            s.setBooksRead(0);
                            s.setMinutesSpent(0);
                            s.setAverageRating(0.0);
                            return s;
                        });

                stats.setBooksRead(stats.getBooksRead() + 1);
            }

            // Tambahkan waktu membaca (setiap sesi dihitung)
            String genreName = null;
            Book bookForSession = bookMapper.findById(session.getBookId());
            if (bookForSession != null) {
                List<Genre> genres = bookMapper.getBookGenres(bookForSession.getId());
                if (!genres.isEmpty()) {
                    genreName = genres.get(0).getName();

                    ReadingStatisticsResponse.GenreStats stats = genreMap.get(genreName);
                    if (stats != null) {
                        int minutesSpent = (session.getTotalDurationSeconds() != null) ?
                                session.getTotalDurationSeconds() / 60 : 0;
                        stats.setMinutesSpent(stats.getMinutesSpent() + minutesSpent);
                    }
                }
            }
        }

        // Hitung persentase
        int totalMinutes = genreMap.values().stream()
                .mapToInt(ReadingStatisticsResponse.GenreStats::getMinutesSpent)
                .sum();

        genreMap.values().forEach(stats -> {
            if (totalMinutes > 0) {
                stats.setPercentage((stats.getMinutesSpent() * 100.0) / totalMinutes);
            } else {
                stats.setPercentage(0.0);
            }

            // Hitung rata-rata rating untuk genre ini
            Double avgRating = calculateAverageRatingForGenre(userId, stats.getGenreName(), since);
            stats.setAverageRating(avgRating != null ? avgRating : 0.0);
        });

        // Urutkan berdasarkan persentase (dari terbesar)
        return genreMap.values().stream()
                .sorted((g1, g2) -> Double.compare(g2.getPercentage(), g1.getPercentage()))
                .collect(Collectors.toList());
    }

    private Double calculateAverageRatingForGenre(Long userId, String genreName, LocalDateTime since) {
        try {
            // Ambil semua buku dengan genre tertentu yang telah dibaca user
            List<Book> booksInGenre = bookMapper.findBooksByGenreAndUser(userId, genreName, since);

            if (booksInGenre.isEmpty()) {
                return null;
            }

            // Hitung total rating
            double totalRating = 0;
            int ratedBooks = 0;

            for (Book book : booksInGenre) {
                BookRating rating = bookRatingMapper.findByUserAndBook(userId, book.getId());
                if (rating != null && rating.getRating() != null) {
                    totalRating += rating.getRating();
                    ratedBooks++;
                }
            }

            if (ratedBooks > 0) {
                return totalRating / ratedBooks;
            }

            return null;
        } catch (Exception e) {
            // Log error jika diperlukan
            return null;
        }
    }

    private List<ReadingStatisticsResponse.TimeSlotStats> buildPeakTimes(
            Long userId, LocalDateTime since) {

        Map<Integer, ReadingStatisticsResponse.TimeSlotStats> timeMap = new HashMap<>();

        // Inisialisasi semua jam (0-23)
        for (int hour = 0; hour < 24; hour++) {
            ReadingStatisticsResponse.TimeSlotStats stats =
                    new ReadingStatisticsResponse.TimeSlotStats();
            stats.setHour(hour);
            stats.setLabel(getTimeOfDayLabel(hour));
            stats.setSessionsCount(0);
            stats.setMinutesRead(0);
            stats.setPercentage(0.0); // Inisialisasi default
            timeMap.put(hour, stats);
        }

        // Ambil semua sesi dan kelompokkan berdasarkan jam
        List<ReadingSession> sessions = sessionMapper.findUserSessionsSince(
                userId, since, 0, Integer.MAX_VALUE);

        for (ReadingSession session : sessions) {
            // Cek null untuk startedAt
            if (session.getStartedAt() == null) {
                continue;
            }

            int hour = session.getStartedAt().getHour();
            ReadingStatisticsResponse.TimeSlotStats stats = timeMap.get(hour);

            if (stats != null) {
                stats.setSessionsCount(stats.getSessionsCount() + 1);

                // Tambahkan waktu membaca (konversi detik ke menit)
                int minutesRead = 0;
                if (session.getTotalDurationSeconds() != null) {
                    minutesRead = session.getTotalDurationSeconds() / 60;
                }
                stats.setMinutesRead(stats.getMinutesRead() + minutesRead);
            }
        }

        // Hitung persentase berdasarkan total menit
        int totalMinutes = timeMap.values().stream()
                .mapToInt(ReadingStatisticsResponse.TimeSlotStats::getMinutesRead)
                .sum();

        // Hitung persentase untuk setiap timeslot
        timeMap.values().forEach(stats -> {
            if (totalMinutes > 0) {
                double percentage = (stats.getMinutesRead() * 100.0) / totalMinutes;
                stats.setPercentage(Math.round(percentage * 100.0) / 100.0); // Bulatkan 2 desimal
            } else {
                stats.setPercentage(0.0);
            }
        });

        // Urutkan berdasarkan jam (0-23)
        return timeMap.values().stream()
                .sorted(Comparator.comparingInt(ReadingStatisticsResponse.TimeSlotStats::getHour))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // ANNOTATIONS HELPERS
    // ═══════════════════════════════════════════════════════════

    private AnnotationItemResponse mapBookmarkToAnnotation(Bookmark bookmark) {
        AnnotationItemResponse item = new AnnotationItemResponse();

        item.setId(bookmark.getId());
        item.setType("bookmark");
        item.setTitle("Bookmark");
        item.setContent(bookmark.getDescription());
        item.setBookId(bookmark.getBookId());
        item.setChapterNumber(bookmark.getChapterNumber());
        item.setPosition(bookmark.getPosition());
        item.setCreatedAt(bookmark.getCreatedAt());
        item.setUpdatedAt(bookmark.getCreatedAt());
        item.setIsPrivate(true);

        Book book = bookMapper.findById(bookmark.getBookId());
        if (book != null) {
            item.setBookTitle(book.getTitle());
            item.setBookSlug(book.getSlug());
            item.setBookCover(book.getCoverImageUrl());
        }

        return item;
    }

// ═══════════════════════════════════════════════════════════
// HELPER METHODS CONTINUATION - Part 2
// ═══════════════════════════════════════════════════════════

    // Continuation from mapHighlightToAnnotation...

    private AnnotationItemResponse mapHighlightToAnnotation(Highlight highlight) {
        AnnotationItemResponse item = new AnnotationItemResponse();

        item.setId(highlight.getId());
        item.setType("highlight");
        item.setTitle("Highlight");
        item.setContent(highlight.getHighlightedText());
        item.setColor(highlight.getColor());
        item.setBookId(highlight.getBookId());
        item.setChapterNumber(highlight.getChapterNumber());
        item.setPosition(highlight.getStartPosition());
        item.setCreatedAt(highlight.getCreatedAt());
        item.setUpdatedAt(highlight.getUpdatedAt());
        item.setIsPrivate(false);

        Book book = bookMapper.findById(highlight.getBookId());
        if (book != null) {
            item.setBookTitle(book.getTitle());
            item.setBookSlug(book.getSlug());
            item.setBookCover(book.getCoverImageUrl());
        }

        return item;
    }

    private AnnotationItemResponse mapNoteToAnnotation(Note note) {
        AnnotationItemResponse item = new AnnotationItemResponse();

        item.setId(note.getId());
        item.setType("note");
        item.setTitle(note.getTitle());
        item.setContent(note.getContent());
        item.setBookId(note.getBookId());
        item.setChapterNumber(note.getChapterNumber());
        item.setPosition(note.getPosition());
        item.setCreatedAt(note.getCreatedAt());
        item.setUpdatedAt(note.getUpdatedAt());
        item.setIsPrivate(note.getIsPrivate());

        Book book = bookMapper.findById(note.getBookId());
        if (book != null) {
            item.setBookTitle(book.getTitle());
            item.setBookSlug(book.getSlug());
            item.setBookCover(book.getCoverImageUrl());
        }

        return item;
    }

    private void sortAnnotations(List<AnnotationItemResponse> items, String sortBy) {
        switch (sortBy) {
            case "recent":
                items.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                break;
            case "book":
                items.sort(Comparator.comparing(AnnotationItemResponse::getBookTitle));
                break;
            case "type":
                items.sort(Comparator.comparing(AnnotationItemResponse::getType));
                break;
            default:
                items.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REVIEW HELPERS
    // ═══════════════════════════════════════════════════════════

    private UserReviewItemResponse mapToUserReviewItem(BookReview review) {
        UserReviewItemResponse item = new UserReviewItemResponse();

        item.setReviewId(review.getId());
        item.setBookId(review.getBookId());
        item.setReviewTitle(review.getTitle());
        item.setReviewContent(review.getContent());

        // Get rating
        BookRating rating = bookRatingMapper.findByUserAndBook(
                review.getUserId(), review.getBookId());
        item.setRating(rating != null ? rating.getRating() : null);

        // Engagement counts (placeholder - would need review_feedback table)
        item.setHelpfulCount(0);
        item.setNotHelpfulCount(0);
        item.setReplyCount(0);

        item.setCreatedAt(review.getCreatedAt());
        item.setUpdatedAt(review.getUpdatedAt());

        Book book = bookMapper.findById(review.getBookId());
        if (book != null) {
            item.setBookTitle(book.getTitle());
            item.setBookSlug(book.getSlug());
            item.setBookCover(book.getCoverImageUrl());
        }

        return item;
    }

    // ═══════════════════════════════════════════════════════════
    // RECOMMENDATION HELPERS
    // ═══════════════════════════════════════════════════════════

    private List<String> getUserFavoriteGenres(Long userId, int limit) {
        List<Map<String, Object>> genreData = genreMapper.getUserFavoriteGenres(
                userId, limit);

        return genreData.stream()
                .map(m -> (String) m.get("name"))
                .collect(Collectors.toList());
    }

    private Double calculateMatchScore(Long userId, BookRecommendationResponse rec) {
        double score = 0.0;

        // Genre match (40%)
        List<String> userGenres = getUserFavoriteGenres(userId, 5);
        if (userGenres.contains(rec.getGenre())) {
            score += 40.0;
        }

        // Rating (30%)
        if (rec.getAverageRating() != null) {
            score += (rec.getAverageRating() / 5.0) * 30.0;
        }

        // Popularity (30%)
        if (rec.getTotalReaders() != null && rec.getTotalReaders() > 100) {
            score += 30.0;
        } else if (rec.getTotalReaders() != null && rec.getTotalReaders() > 50) {
            score += 20.0;
        } else if (rec.getTotalReaders() != null && rec.getTotalReaders() > 10) {
            score += 10.0;
        }

        return score;
    }

    private List<String> identifyMatchingFactors(Long userId,
                                                 BookRecommendationResponse rec) {

        List<String> factors = new ArrayList<>();

        List<String> userGenres = getUserFavoriteGenres(userId, 5);
        if (userGenres.contains(rec.getGenre())) {
            factors.add("genre");
        }

        if (rec.getAverageRating() != null && rec.getAverageRating() >= 4.0) {
            factors.add("rating");
        }

        if (rec.getTotalReaders() != null && rec.getTotalReaders() > 100) {
            factors.add("popular");
        }

        return factors;
    }

    // ═══════════════════════════════════════════════════════════
    // CALENDAR HELPERS
    // ═══════════════════════════════════════════════════════════

    private ReadingCalendarResponse.CalendarDay buildCalendarDay(
            Long userId, LocalDate date) {

        ReadingCalendarResponse.CalendarDay day = new ReadingCalendarResponse.CalendarDay();

        day.setDate(date.toString());

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        // Ambil sesi untuk hari ini
        List<ReadingSession> sessions = sessionMapper.findUserSessionsBetween(
                userId, dayStart, dayEnd);

        // Hitung total menit membaca
        int totalMinutes = sessions.stream()
                .mapToInt(s -> {
                    if (s.getTotalDurationSeconds() != null) {
                        // Konversi detik ke menit
                        return s.getTotalDurationSeconds() / 60;
                    }
                    return 0;
                })
                .sum();

        day.setMinutesRead(totalMinutes);
        day.setHasActivity(totalMinutes > 0);

        // Hitung intensitas (0-4 untuk visualisasi)
        if (totalMinutes == 0) {
            day.setIntensity(0);
        } else if (totalMinutes < 15) {
            day.setIntensity(1);
        } else if (totalMinutes < 30) {
            day.setIntensity(2);
        } else if (totalMinutes < 60) {
            day.setIntensity(3);
        } else {
            day.setIntensity(4);
        }

        // Buat daftar aktivitas
        List<String> activities = new ArrayList<>();
        if (totalMinutes > 0) {
            activities.add(String.format("Membaca %d menit", totalMinutes));

            // Tambahkan info jumlah bab yang dibaca jika tersedia
            int totalChapters = sessions.stream()
                    .mapToInt(s -> s.getChaptersRead() != null ? s.getChaptersRead() : 0)
                    .sum();
            if (totalChapters > 0) {
                activities.add(String.format("%d bab", totalChapters));
            }
        }

        // Cek aktivitas lain (bookmark, highlight, dll)
        try {
            // Bookmark
            List<Bookmark> bookmarks = bookmarkMapper.findByUserSince(userId, dayStart);
            if (bookmarks != null) {
                long bookmarkCount = bookmarks.stream()
                        .filter(b -> b != null && b.getCreatedAt() != null)
                        .filter(b -> b.getCreatedAt().isAfter(dayStart) &&
                                b.getCreatedAt().isBefore(dayEnd))
                        .count();
                if (bookmarkCount > 0) {
                    activities.add(String.format("%d bookmark", bookmarkCount));
                }
            }

            // Highlight (jika ada)
            if (highlightMapper != null) {
                List<Highlight> highlights = highlightMapper.findByUserSince(userId, dayStart);
                if (highlights != null) {
                    long highlightCount = highlights.stream()
                            .filter(h -> h != null && h.getCreatedAt() != null)
                            .filter(h -> h.getCreatedAt().isAfter(dayStart) &&
                                    h.getCreatedAt().isBefore(dayEnd))
                            .count();
                    if (highlightCount > 0) {
                        activities.add(String.format("%d highlight", highlightCount));
                    }
                }
            }

            // Note (jika ada)
            if (noteMapper != null) {
                List<Note> notes = noteMapper.findByUserSince(userId, dayStart);
                if (notes != null) {
                    long noteCount = notes.stream()
                            .filter(n -> n != null && n.getCreatedAt() != null)
                            .filter(n -> n.getCreatedAt().isAfter(dayStart) &&
                                    n.getCreatedAt().isBefore(dayEnd))
                            .count();
                    if (noteCount > 0) {
                        activities.add(String.format("%d catatan", noteCount));
                    }
                }
            }
        } catch (Exception e) {
            // Log error jika diperlukan, tapi lanjutkan proses
            // System.err.println("Error fetching additional activities: " + e.getMessage());
        }

        // Hitung jumlah sesi membaca
        int sessionCount = sessions.size();
        if (sessionCount > 0) {
            activities.add(String.format("%d sesi", sessionCount));
        }

        day.setActivities(activities);

        return day;
    }

    private Integer calculateStreakInPeriod(
            List<ReadingCalendarResponse.CalendarDay> days) {

        int maxStreak = 0;
        int currentStreak = 0;

        for (ReadingCalendarResponse.CalendarDay day : days) {
            if (day.getHasActivity()) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }

        return maxStreak;
    }

    // ═══════════════════════════════════════════════════════════
    // ACHIEVEMENT HELPERS
    // ═══════════════════════════════════════════════════════════

    private List<AchievementResponse> calculateReadingAchievements(Long userId) {
        List<AchievementResponse> achievements = new ArrayList<>();

        int totalBooks = chapterProgressMapper.countCompletedBooks(userId);
        Integer totalMinutes = activityMapper.getUserTotalReadingMinutes(userId);
        int totalHours = totalMinutes != null ? totalMinutes / 60 : 0;

        // First Book
        achievements.add(createAchievement(
                "first_book", "reading", "First Book",
                "Complete your first book", "bronze",
                1, totalBooks, 1));

        // Bookworm - 10 books
        achievements.add(createAchievement(
                "bookworm", "reading", "Bookworm",
                "Complete 10 books", "silver",
                10, totalBooks, 10));

        // Book Master - 50 books
        achievements.add(createAchievement(
                "book_master", "reading", "Book Master",
                "Complete 50 books", "gold",
                50, totalBooks, 20));

        // 10 Hours
        achievements.add(createAchievement(
                "10_hours", "reading", "10 Hours",
                "Read for 10 hours", "bronze",
                10, totalHours, 5));

        // 100 Hours
        achievements.add(createAchievement(
                "100_hours", "reading", "Century Reader",
                "Read for 100 hours", "silver",
                100, totalHours, 15));

        // 1000 Hours
        achievements.add(createAchievement(
                "1000_hours", "reading", "Millennium Reader",
                "Read for 1000 hours", "gold",
                1000, totalHours, 30));

        return achievements;
    }

    private List<AchievementResponse> calculateStreakAchievements(Long userId) {
        List<AchievementResponse> achievements = new ArrayList<>();

        Integer currentStreak = calculateCurrentStreak(userId);
        Integer longestStreak = calculateLongestStreak(userId);

        // 7-day streak
        achievements.add(createAchievement(
                "streak_7", "reading", "Week Warrior",
                "Read 7 days in a row", "bronze",
                7, currentStreak, 5));

        // 30-day streak
        achievements.add(createAchievement(
                "streak_30", "reading", "Monthly Master",
                "Read 30 days in a row", "silver",
                30, currentStreak, 15));

        // 100-day streak
        achievements.add(createAchievement(
                "streak_100", "reading", "Centurion",
                "Read 100 days in a row", "gold",
                100, longestStreak, 25));

        return achievements;
    }

    private List<AchievementResponse> calculateSocialAchievements(Long userId) {
        List<AchievementResponse> achievements = new ArrayList<>();

        int reviewCount = bookReviewMapper.countByUser(userId);
        int highlightCount = highlightMapper.countByUser(userId);

        // First Review
        achievements.add(createAchievement(
                "first_review", "social", "Critic's Corner",
                "Write your first review", "bronze",
                1, reviewCount, 5));

        // 10 Reviews
        achievements.add(createAchievement(
                "10_reviews", "social", "Book Critic",
                "Write 10 reviews", "silver",
                10, reviewCount, 10));

        // Highlighter
        achievements.add(createAchievement(
                "highlighter", "contribution", "Highlighter",
                "Create 50 highlights", "silver",
                50, highlightCount, 10));

        return achievements;
    }

    private AchievementResponse createAchievement(
            String id, String category, String title, String description,
            String tier, int target, int current, int points) {

        AchievementResponse achievement = new AchievementResponse();

        achievement.setAchievementId(id);
        achievement.setCategory(category);
        achievement.setTitle(title);
        achievement.setDescription(description);
        achievement.setTier(tier);
        achievement.setPoints(points);
        achievement.setTargetProgress(target);
        achievement.setCurrentProgress(Math.min(current, target));
        achievement.setProgressPercentage((current * 100.0) / target);
        achievement.setIsUnlocked(current >= target);

        if (current >= target) {
            // Would normally get from database
            achievement.setUnlockedAt(LocalDateTime.now());
        }

        // Badge URL
        achievement.setBadgeUrl(
                String.format("/badges/%s_%s.png", id, tier));

        // Rarity (simplified)
        achievement.setRarityPercentage(
                tier.equals("bronze") ? 60 : tier.equals("silver") ? 30 : 10);

        return achievement;
    }

    private UserReadingDashboardResponse.RecentAchievement mapToRecentAchievement(
            AchievementResponse achievement) {

        UserReadingDashboardResponse.RecentAchievement recent =
                new UserReadingDashboardResponse.RecentAchievement();

        recent.setAchievementId(achievement.getAchievementId());
        recent.setTitle(achievement.getTitle());
        recent.setDescription(achievement.getDescription());
        recent.setBadgeUrl(achievement.getBadgeUrl());
        recent.setUnlockedAt(achievement.getUnlockedAt());

        return recent;
    }

    // ═══════════════════════════════════════════════════════════
    // MAPPING TO RECENT ANNOTATIONS
    // ═══════════════════════════════════════════════════════════

    private UserReadingDashboardResponse.RecentAnnotation mapBookmarkToRecentAnnotation(
            Bookmark bookmark) {

        UserReadingDashboardResponse.RecentAnnotation annotation =
                new UserReadingDashboardResponse.RecentAnnotation();

        annotation.setType("bookmark");
        annotation.setContent(bookmark.getDescription());
        annotation.setCreatedAt(bookmark.getCreatedAt());
        annotation.setChapterNumber(bookmark.getChapterNumber());

        Book book = bookMapper.findById(bookmark.getBookId());
        if (book != null) {
            annotation.setBookTitle(book.getTitle());
            annotation.setBookSlug(book.getSlug());
        }

        return annotation;
    }

    private UserReadingDashboardResponse.RecentAnnotation mapHighlightToRecentAnnotation(
            Highlight highlight) {

        UserReadingDashboardResponse.RecentAnnotation annotation =
                new UserReadingDashboardResponse.RecentAnnotation();

        annotation.setType("highlight");
        annotation.setContent(highlight.getHighlightedText());
        annotation.setCreatedAt(highlight.getCreatedAt());
        annotation.setChapterNumber(highlight.getChapterNumber());

        Book book = bookMapper.findById(highlight.getBookId());
        if (book != null) {
            annotation.setBookTitle(book.getTitle());
            annotation.setBookSlug(book.getSlug());
        }

        return annotation;
    }

    private UserReadingDashboardResponse.RecentAnnotation mapNoteToRecentAnnotation(
            Note note) {

        UserReadingDashboardResponse.RecentAnnotation annotation =
                new UserReadingDashboardResponse.RecentAnnotation();

        annotation.setType("note");
        annotation.setContent(note.getContent());
        annotation.setCreatedAt(note.getCreatedAt());
        annotation.setChapterNumber(note.getChapterNumber());

        Book book = bookMapper.findById(note.getBookId());
        if (book != null) {
            annotation.setBookTitle(book.getTitle());
            annotation.setBookSlug(book.getSlug());
        }

        return annotation;
    }

    // ═══════════════════════════════════════════════════════════
    // STREAK CALCULATIONS
    // ═══════════════════════════════════════════════════════════

    private Integer calculateCurrentStreak(Long userId) {
        LocalDate today = LocalDate.now();
        int streak = 0;

        // Check if user has activity today
        LocalDateTime todayStart = today.atStartOfDay();
        Boolean hasToday = activityMapper.hasActivitySince(userId, todayStart);

        if (!hasToday) {
            // Check yesterday
            LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
            Boolean hasYesterday = activityMapper.hasActivitySince(userId, yesterdayStart);
            if (!hasYesterday) {
                return 0; // Streak broken
            }
        }

        // Count backwards from today
        LocalDate checkDate = today;
        while (true) {
            LocalDateTime dayStart = checkDate.atStartOfDay();
            LocalDateTime dayEnd = checkDate.plusDays(1).atStartOfDay();

            List<ReadingSession> sessions = sessionMapper.findUserSessionsBetween(
                    userId, dayStart, dayEnd);

            if (sessions.isEmpty()) {
                break;
            }

            streak++;
            checkDate = checkDate.minusDays(1);

            // Limit to reasonable check (e.g., 365 days)
            if (streak > 365) break;
        }

        return streak;
    }

    private Integer calculateLongestStreak(Long userId) {
        // Simplified - would need more efficient query in production
        List<ReadingSession> allSessions = sessionMapper.findUserSessionsSince(
                userId, LocalDateTime.now().minusYears(2), 0, Integer.MAX_VALUE);

        if (allSessions.isEmpty()) return 0;

        // Group by date
        Map<LocalDate, Boolean> activityMap = new HashMap<>();
        allSessions.forEach(session ->
                activityMap.put(session.getStartedAt().toLocalDate(), true));

        // Find longest streak
        int maxStreak = 0;
        int currentStreak = 0;

        LocalDate startDate = allSessions.stream()
                .map(s -> s.getStartedAt().toLocalDate())
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        for (LocalDate date = startDate; !date.isAfter(LocalDate.now());
             date = date.plusDays(1)) {

            if (activityMap.containsKey(date)) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }

        return maxStreak;
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITY HELPERS
    // ═══════════════════════════════════════════════════════════

    private boolean isBookInProgress(Long userId, Long bookId) {
        List<ChapterProgress> progress =
                chapterProgressMapper.findAllByUserAndBook(userId, bookId);

        if (progress.isEmpty()) return false;

        long completed = progress.stream()
                .filter(ChapterProgress::getIsCompleted)
                .count();

        Book book = bookMapper.findById(bookId);
        if (book == null) return false;

        return completed > 0 && completed < book.getTotalPages();
    }

    private boolean isBookCompleted(Long userId, Long bookId) {
        Book book = bookMapper.findById(bookId);
        if (book == null) return false;

        List<ChapterProgress> progress =
                chapterProgressMapper.findAllByUserAndBook(userId, bookId);

        long completed = progress.stream()
                .filter(ChapterProgress::getIsCompleted)
                .count();

        return completed >= book.getTotalPages();
    }

    private String getTimeOfDayLabel(int hour) {
        if (hour >= 5 && hour < 12) return "Morning";
        if (hour >= 12 && hour < 17) return "Afternoon";
        if (hour >= 17 && hour < 21) return "Evening";
        return "Night";
    }

    private String getPaceLabel(double chaptersPerDay) {
        if (chaptersPerDay < 0.5) return "Slow";
        if (chaptersPerDay < 2.0) return "Moderate";
        return "Fast";
    }

    private String formatReadingTime(Integer minutes) {
        if (minutes == null || minutes == 0) return "0m";

        int hours = minutes / 60;
        int mins = minutes % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, mins);
        }
        return String.format("%dm", mins);
    }

    private Integer getIntValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Double) return ((Double) value).intValue();
        return 0;
    }

    private Double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        return 0.0;
    }

    // ═══════════════════════════════════════════════════════════
    // AUTHENTICATION HELPER
    // ═══════════════════════════════════════════════════════════

    private User getCurrentUser() {
        // Perbaikan 1: Gunakan getUsername() bukan getCurrentUsername()
        String username = headerHolder.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new UnauthorizedException();
        }

        // Perbaikan 2: Gunakan findUserByUsername() bukan findByUsername()
        User user = userMapper.findUserByUsername(username);
        if (user == null) {
            throw new UnauthorizedException();
        }

        return user;
    }
}