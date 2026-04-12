// ============================================================
// PATCH: DashboardServiceImpl.java
// Tambahkan method parseCfiChapter() dan gunakan di semua
// tempat yang mengambil "nomor bab" dari progress/session.
// ============================================================

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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper               userMapper;
    private final BookMapper               bookMapper;
    private final EpubAnnotationMapper     epubAnnotationMapper;
    private final EpubBookmarkMapper       epubBookmarkMapper;
    private final ReadingSessionMapper     sessionMapper;
    private final ReadingProgressMapper    readingProgressMapper;
    private final ChapterProgressMapper    chapterProgressMapper;
    private final ReadingActivityMapper    activityMapper;
    private final UserReadingPatternMapper patternMapper;
    private final ChapterRatingMapper      ratingMapper;
    private final ChapterReviewMapper      reviewMapper;
    private final SearchMapper             searchMapper;
    private final HeaderHolder             headerHolder;

    private static final String SESSION_TYPE_EPUB    = "EPUB";
    private static final String SESSION_TYPE_CHAPTER = "CHAPTER";
    private static final String SUCCESS              = "Success";

    // ─────────────────────────────────────────────────────────
    // FIX #2 (BACKEND): Parse spine index dari epubcfi(/6/N!/...)
    // epubcfi(/6/14!/4/2/66/1:0) → spineStep=14 → chapter=7 (14/2)
    // ─────────────────────────────────────────────────────────
    private int parseCfiChapter(String cfi) {
        if (cfi == null || cfi.isBlank()) return -1;
        try {
            String base      = cfi.split("!")[0];                  // "epubcfi(/6/14"
            String inner     = base.replace("epubcfi(", "");       // "/6/14"
            String[] parts   = inner.split("/");                   // ["","6","14"]
            if (parts.length < 3) return -1;
            int spineStep    = Integer.parseInt(parts[2]);         // 14
            if (spineStep < 2) return -1;
            return spineStep / 2;                                  // 7
        } catch (Exception e) {
            log.debug("parseCfiChapter failed for cfi={}: {}", cfi, e.getMessage());
            return -1;
        }
    }

    // ─────────────────────────────────────────────────────────
    // Tentukan nomor bab terbaik:
    //   1. Dari CFI (prioritas tertinggi)
    //   2. Dari currentPage di reading_progress
    //   3. Dari startChapter di reading_session
    // ─────────────────────────────────────────────────────────
    private int resolveChapter(ReadingProgress progress, ReadingSession session) {
        String cfi       = progress != null ? progress.getCurrentPosition() : null;
        int    cfiChapter = parseCfiChapter(cfi);

        if (cfiChapter > 0) return cfiChapter;

        if (progress != null
                && progress.getCurrentPage() != null
                && progress.getCurrentPage() > 0) {
            return progress.getCurrentPage();
        }

        return session != null && session.getStartChapter() != null
                ? session.getStartChapter() : 0;
    }

    private User getCurrentUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.trim().isEmpty()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private boolean isEpubSession(ReadingSession session) {
        if (session == null) return false;
        if (SESSION_TYPE_EPUB.equals(session.getSessionType())) return true;
        if (session.getSessionType() == null) {
            return (session.getStartChapter() == null || session.getStartChapter() == 0)
                    && (session.getEndChapter()   == null || session.getEndChapter()   == 0)
                    && (session.getChaptersRead() == null || session.getChaptersRead() == 0);
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════
    // MAIN DASHBOARD
    // ═════════════════════════════════════════════════════════

    @Override
    public DataResponse<DashboardMainResponse> getMainDashboard() {
        User user = getCurrentUser();

        DashboardMainResponse response = new DashboardMainResponse();
        response.setOverviewStats(buildOverviewStats(user.getId()));
        response.setBooksInProgress(buildBooksInProgress(user.getId()));
        response.setReadingPattern(buildReadingPatternSummary(user.getId()));
        response.setRecentlyRead(buildRecentlyRead(user.getId()));
        response.setAnnotationsSummary(buildAnnotationsSummary(user.getId()));
        response.setRecentAchievements(List.of());

        return new DataResponse<>(SUCCESS, "Dashboard retrieved", HttpStatus.OK.value(), response);
    }

    private OverviewStats buildOverviewStats(Long userId) {
        OverviewStats stats = new OverviewStats();

        List<ReadingSession> allSessions = sessionMapper.findAllUserSessions(userId);

        long totalBooks = allSessions.stream()
                .map(ReadingSession::getBookId).filter(Objects::nonNull).distinct().count();
        stats.setTotalBooks((int) totalBooks);

        List<ReadingProgress> allProgress = readingProgressMapper.findAllByUser(userId);
        long completed = allProgress.stream()
                .filter(p -> p.getPercentageCompleted() != null
                        && p.getPercentageCompleted().doubleValue() >= 95.0)
                .count();
        stats.setBooksCompleted((int) completed);

        int totalSeconds = allSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0)
                .sum();
        stats.setTotalReadingTimeHours(totalSeconds / 3600);

        stats.setCurrentStreak(calculateCurrentStreak(allSessions));
        stats.setLongestStreak(calculateLongestStreak(allSessions));

        List<Map<String, Object>> ratingData = ratingMapper.findAllUserRatings(userId);
        stats.setAverageRating(ratingData.isEmpty() ? 0.0
                : ratingData.stream().mapToInt(r -> safeInt(r.get("rating"))).average().orElse(0.0));

        stats.setCompletionRate(allProgress.isEmpty() ? 0.0
                : allProgress.stream()
                .mapToDouble(p -> p.getPercentageCompleted() != null
                        ? p.getPercentageCompleted().doubleValue() : 0.0)
                .average().orElse(0.0));

        return stats;
    }

    private List<BookInProgressResponse> buildBooksInProgress(Long userId) {
        List<ReadingSession> latestSessions = sessionMapper.findLatestSessionPerBook(userId);

        return latestSessions.stream()
                .map(session -> {
                    Book book = bookMapper.findById(session.getBookId());
                    if (book == null) return null;

                    ReadingProgress progress =
                            readingProgressMapper.findByUserAndBook(userId, book.getId());
                    double pct = progress != null && progress.getPercentageCompleted() != null
                            ? progress.getPercentageCompleted().doubleValue() : 0.0;

                    if (pct >= 95.0) return null;

                    // FIX #2: resolveChapter pakai CFI lebih dulu
                    int displayChapter = resolveChapter(progress, session);

                    BookInProgressResponse r = new BookInProgressResponse();
                    r.setBookId(book.getId());
                    r.setBookSlug(book.getSlug());
                    r.setBookTitle(book.getTitle());
                    r.setAuthorName(getAuthorName(book.getId()));
                    r.setCoverImageUrl(book.getCoverImageUrl());
                    r.setProgressPercentage(pct);
                    r.setLastReadAt(session.getStartedAt());
                    r.setCurrentChapter(displayChapter);

                    if (progress != null) {
                        r.setTotalChapters(progress.getTotalPages() != null ? progress.getTotalPages() : 0);
                        r.setLastCfi(progress.getCurrentPosition());
                    } else {
                        r.setTotalChapters(0);
                        r.setLastCfi(null);
                    }

                    return r;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BookInProgressResponse::getLastReadAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList());
    }

    private ReadingPatternSummary buildReadingPatternSummary(Long userId) {
        ReadingPatternSummary summary = new ReadingPatternSummary();
        List<ReadingSession> allSessions = sessionMapper.findAllUserSessions(userId);

        if (allSessions.isEmpty()) {
            summary.setPreferredReadingTime("Belum ada data");
            summary.setAverageReadingSpeedWpm(0);
            summary.setAverageSessionMinutes(0);
            summary.setReadingPace("Belum ada data");
            return summary;
        }

        Map<Integer, Long> hourCount = allSessions.stream()
                .filter(s -> s.getStartedAt() != null)
                .collect(Collectors.groupingBy(s -> s.getStartedAt().getHour(), Collectors.counting()));

        int preferredHour = hourCount.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(20);

        summary.setPreferredReadingTime(getReadingTimeLabel(preferredHour));

        double avgDuration = allSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0)
                .average().orElse(0.0);
        summary.setAverageSessionMinutes((int) (avgDuration / 60));
        summary.setAverageReadingSpeedWpm(0);

        int totalSeconds = allSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0)
                .sum();
        long totalDays = ChronoUnit.DAYS.between(
                allSessions.stream().map(ReadingSession::getStartedAt).filter(Objects::nonNull)
                        .min(Comparator.naturalOrder()).orElse(LocalDateTime.now()),
                LocalDateTime.now());
        double chaptersPerDay = totalDays > 0 ? (totalSeconds / 300.0) / totalDays : 0;
        summary.setReadingPace(getReadingPaceLabel(chaptersPerDay));

        return summary;
    }

    private List<RecentlyReadResponse> buildRecentlyRead(Long userId) {
        List<ReadingSession> recentSessions = sessionMapper.findRecentUserSessions(userId, 6);

        Map<Long, ReadingSession> latestPerBook = new LinkedHashMap<>();
        for (ReadingSession session : recentSessions) {
            latestPerBook.putIfAbsent(session.getBookId(), session);
        }

        return latestPerBook.values().stream()
                .map(session -> {
                    Book book = bookMapper.findById(session.getBookId());
                    if (book == null) return null;

                    ReadingProgress progress =
                            readingProgressMapper.findByUserAndBook(userId, book.getId());
                    double pct = progress != null && progress.getPercentageCompleted() != null
                            ? progress.getPercentageCompleted().doubleValue() : 0.0;

                    // FIX #2: resolveChapter pakai CFI lebih dulu
                    int displayChapter = resolveChapter(progress, session);

                    RecentlyReadResponse r = new RecentlyReadResponse();
                    r.setBookId(book.getId());
                    r.setBookSlug(book.getSlug());
                    r.setBookTitle(book.getTitle());
                    r.setAuthorName(getAuthorName(book.getId()));
                    r.setCoverImageUrl(book.getCoverImageUrl());
                    r.setLastReadAt(session.getStartedAt());
                    r.setActivityType(pct >= 95.0 ? "completed" : pct > 0 ? "reading" : "started");
                    r.setCurrentChapter(displayChapter);

                    if (progress != null) {
                        r.setTotalChapters(progress.getTotalPages() != null ? progress.getTotalPages() : 0);
                        r.setProgressPercentage(pct);
                        r.setLastCfi(progress.getCurrentPosition());
                    } else {
                        r.setTotalChapters(0);
                        r.setProgressPercentage(pct);
                        r.setLastCfi(null);
                    }

                    return r;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private AnnotationsSummary buildAnnotationsSummary(Long userId) {
        AnnotationsSummary summary = new AnnotationsSummary();
        summary.setTotalBookmarks(epubBookmarkMapper.countByUser(userId));
        summary.setTotalHighlights(epubAnnotationMapper.countHighlightsByUser(userId));
        summary.setTotalNotes(epubAnnotationMapper.countNotesByUser(userId));
        summary.setTotalReviews(reviewMapper.countByUser(userId));
        return summary;
    }

    // ═════════════════════════════════════════════════════════
    // ANNOTATIONS PAGE
    // ═════════════════════════════════════════════════════════

    @Override
    public DataResponse<AnnotationsPageResponse> getAnnotations(
            String type, int page, int limit, String sortBy) {

        User user  = getCurrentUser();
        int offset = (page - 1) * limit;

        List<AnnotationItemResponse> items      = new ArrayList<>();
        int                          totalCount = 0;

        if ("all".equals(type) || "highlight".equals(type) || "note".equals(type)) {
            List<EpubAnnotation> epubAnnotations =
                    epubAnnotationMapper.findByUserPaged(user.getId(), offset, limit);

            for (EpubAnnotation ann : epubAnnotations) {
                boolean hasNote = ann.getNote() != null && !ann.getNote().isBlank();
                if ("highlight".equals(type) && hasNote)  continue;
                if ("note".equals(type)      && !hasNote) continue;

                Book book = bookMapper.findById(ann.getBookId());
                AnnotationItemResponse item = new AnnotationItemResponse();
                item.setId(ann.getId());
                item.setType(hasNote ? "note" : "highlight");
                item.setContent(ann.getSelectedText());
                item.setCfi(ann.getCfi());
                item.setCreatedAt(ann.getCreatedAt());
                if (book != null) {
                    item.setBookId(book.getId());
                    item.setBookSlug(book.getSlug());
                    item.setBookTitle(book.getTitle());
                    item.setBookCover(book.getCoverImageUrl());
                }
                item.setChapterNumber(null);
                items.add(item);
            }

            if ("highlight".equals(type)) {
                totalCount += epubAnnotationMapper.countHighlightsByUser(user.getId());
            } else if ("note".equals(type)) {
                totalCount += epubAnnotationMapper.countNotesByUser(user.getId());
            } else {
                totalCount += epubAnnotationMapper.countByUser(user.getId());
            }
        }

        if ("all".equals(type) || "bookmark".equals(type)) {
            List<EpubBookmark> epubBookmarks =
                    epubBookmarkMapper.findByUser(user.getId(), offset, limit);

            for (EpubBookmark bm : epubBookmarks) {
                Book book = bookMapper.findById(bm.getBookId());
                AnnotationItemResponse item = new AnnotationItemResponse();
                item.setId(bm.getId());
                item.setType("bookmark");
                item.setContent(bm.getLabel());
                item.setCfi(bm.getCfi());
                item.setCreatedAt(bm.getCreatedAt());
                if (book != null) {
                    item.setBookId(book.getId());
                    item.setBookSlug(book.getSlug());
                    item.setBookTitle(book.getTitle());
                    item.setBookCover(book.getCoverImageUrl());
                }
                item.setChapterNumber(null);
                items.add(item);
            }

            totalCount += epubBookmarkMapper.countByUser(user.getId());
        }

        Comparator<AnnotationItemResponse> comparator = "book".equals(sortBy)
                ? Comparator.comparing(AnnotationItemResponse::getBookTitle,
                Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(AnnotationItemResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder()));
        items.sort(comparator);

        AnnotationsPageResponse pageResponse = new AnnotationsPageResponse();
        pageResponse.setItems(items);
        pageResponse.setTotal(totalCount);
        pageResponse.setPage(page);
        pageResponse.setLimit(limit);

        return new DataResponse<>(SUCCESS, "Annotations retrieved", HttpStatus.OK.value(), pageResponse);
    }

    // ═════════════════════════════════════════════════════════
    // LIBRARY PAGE
    // ═════════════════════════════════════════════════════════

    @Override
    public DataResponse<LibraryPageResponse> getLibrary(
            String filter, int page, int limit, String sortBy) {

        User user = getCurrentUser();

        List<ReadingSession> allSessions = sessionMapper.findAllUserSessions(user.getId());

        Map<Long, ReadingSession> latestPerBook = new LinkedHashMap<>();
        for (ReadingSession session : allSessions) {
            latestPerBook.merge(session.getBookId(), session, (existing, incoming) -> {
                if (incoming.getStartedAt() != null && existing.getStartedAt() != null) {
                    return incoming.getStartedAt().isAfter(existing.getStartedAt()) ? incoming : existing;
                }
                return existing;
            });
        }

        List<LibraryBookResponse> books = latestPerBook.entrySet().stream()
                .map(entry -> {
                    Book book = bookMapper.findById(entry.getKey());
                    if (book == null) return null;

                    ReadingProgress progress =
                            readingProgressMapper.findByUserAndBook(user.getId(), book.getId());
                    double pct = progress != null && progress.getPercentageCompleted() != null
                            ? progress.getPercentageCompleted().doubleValue() : 0.0;

                    String status = pct >= 95.0 ? "completed" : pct > 0 ? "reading" : "not_started";

                    if ("reading".equals(filter)   && !"reading".equals(status))   return null;
                    if ("completed".equals(filter) && !"completed".equals(status)) return null;
                    if ("bookmarked".equals(filter)) {
                        if (epubBookmarkMapper.countByUserAndBook(user.getId(), book.getId()) == 0)
                            return null;
                    }

                    ReadingSession session     = entry.getValue();
                    int epubBookmarkCount  = epubBookmarkMapper.countByUserAndBook(user.getId(), book.getId());
                    int epubHighlightCount = epubAnnotationMapper.countByUserAndBook(user.getId(), book.getId());

                    // FIX #2: resolveChapter pakai CFI lebih dulu
                    int displayChapter = resolveChapter(progress, session);

                    LibraryBookResponse r = new LibraryBookResponse();
                    r.setBookId(book.getId());
                    r.setBookSlug(book.getSlug());
                    r.setBookTitle(book.getTitle());
                    r.setAuthorName(getAuthorName(book.getId()));
                    r.setCoverImageUrl(book.getCoverImageUrl());
                    r.setProgressPercentage(pct);
                    r.setReadingStatus(status);
                    r.setLastReadAt(session.getStartedAt());
                    r.setBookmarkCount(epubBookmarkCount);
                    r.setHighlightCount(epubHighlightCount);
                    r.setNoteCount(0);
                    r.setCurrentChapter(displayChapter);

                    if (progress != null) {
                        r.setTotalChapters(progress.getTotalPages() != null ? progress.getTotalPages() : 0);
                        r.setLastCfi(progress.getCurrentPosition());
                    } else {
                        r.setTotalChapters(0);
                        r.setLastCfi(null);
                    }

                    return r;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Comparator<LibraryBookResponse> comparator = switch (sortBy) {
            case "progress" -> Comparator.comparing(
                    LibraryBookResponse::getProgressPercentage, Comparator.reverseOrder());
            case "title"    -> Comparator.comparing(
                    LibraryBookResponse::getBookTitle, Comparator.nullsLast(Comparator.naturalOrder()));
            default         -> Comparator.comparing(
                    LibraryBookResponse::getLastReadAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        books.sort(comparator);

        int total     = books.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex   = Math.min(fromIndex + limit, total);

        LibraryPageResponse pageResponse = new LibraryPageResponse();
        pageResponse.setItems(books.subList(fromIndex, toIndex));
        pageResponse.setTotalData(total);
        pageResponse.setPage(page);
        pageResponse.setLimit(limit);

        return new DataResponse<>(SUCCESS, "Library retrieved", HttpStatus.OK.value(), pageResponse);
    }

    // ═════════════════════════════════════════════════════════
    // READING HISTORY
    // ═════════════════════════════════════════════════════════

    @Override
    public DataResponse<ReadingHistoryPageResponse> getReadingHistory(int days, int page, int limit) {
        User user = getCurrentUser();

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<ReadingSession> sessions = sessionMapper.findUserSessionsSince(user.getId(), since);

        sessions.sort(Comparator.comparing(ReadingSession::getStartedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<ReadingHistoryItemResponse> items = sessions.stream()
                .map(session -> {
                    Book book = bookMapper.findById(session.getBookId());
                    if (book == null) return null;

                    boolean epub = isEpubSession(session);

                    ReadingProgress progress =
                            readingProgressMapper.findByUserAndBook(user.getId(), book.getId());

                    // FIX #2: resolveChapter pakai CFI lebih dulu
                    int lastKnownChapter = resolveChapter(progress, session);

                    ReadingHistoryItemResponse item = new ReadingHistoryItemResponse();
                    item.setActivityId(session.getId());
                    item.setActivityType("reading_session");
                    item.setBookId(book.getId());
                    item.setBookSlug(book.getSlug());
                    item.setBookTitle(book.getTitle());
                    item.setAuthorName(getAuthorName(book.getId()));
                    item.setBookCover(book.getCoverImageUrl());
                    item.setTimestamp(session.getStartedAt());
                    item.setChapterNumber(lastKnownChapter > 0 ? lastKnownChapter : null);

                    int durationSec = session.getTotalDurationSeconds() != null
                            ? session.getTotalDurationSeconds() : 0;
                    String durationStr = durationSec >= 3600
                            ? String.format("%d jam %d menit", durationSec / 3600, (durationSec % 3600) / 60)
                            : durationSec >= 60
                            ? String.format("%d menit", durationSec / 60)
                            : durationSec > 0
                            ? String.format("%d detik", durationSec)
                            : "kurang dari 1 menit";

                    // FIX #2: deskripsi sekarang pakai bab dari CFI
                    if (epub) {
                        item.setDescription(String.format(
                                "Membaca EPUB (bab %d) selama %s",
                                lastKnownChapter > 0 ? lastKnownChapter : 0, durationStr));
                    } else {
                        item.setDescription(String.format(
                                "Membaca hingga Bab %d selama %s",
                                lastKnownChapter > 0 ? lastKnownChapter : 0, durationStr));
                    }

                    item.setProgressPercentage(
                            progress != null && progress.getPercentageCompleted() != null
                                    ? progress.getPercentageCompleted().doubleValue() : 0.0);

                    item.setLastCfi(
                            progress != null ? progress.getCurrentPosition() : null);

                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int total     = items.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex   = Math.min(fromIndex + limit, total);

        ReadingHistoryPageResponse pageResponse = new ReadingHistoryPageResponse();
        pageResponse.setList(items.subList(fromIndex, toIndex));
        pageResponse.setTotal(total);
        pageResponse.setPage(page);
        pageResponse.setLimit(limit);

        return new DataResponse<>(SUCCESS, "Reading history retrieved", HttpStatus.OK.value(), pageResponse);
    }

    // ═════════════════════════════════════════════════════════
    // STATISTICS
    // ═════════════════════════════════════════════════════════

    @Override
    public DataResponse<StatisticsResponse> getStatistics(int period) {
        User user  = getCurrentUser();
        LocalDateTime since = LocalDateTime.now().minusDays(period);

        List<ReadingSession> sessions = sessionMapper.findUserSessionsSince(user.getId(), since);

        StatisticsResponse stats = new StatisticsResponse();

        long totalBooks = sessions.stream()
                .map(ReadingSession::getBookId).filter(Objects::nonNull).distinct().count();
        stats.setTotalBooksRead((int) totalBooks);

        int totalSeconds = sessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0)
                .sum();
        stats.setTotalReadingMinutes(totalSeconds / 60);

        long totalChapters = sessions.stream()
                .filter(s -> !isEpubSession(s))
                .mapToInt(s -> {
                    int start = s.getStartChapter() != null ? s.getStartChapter() : 0;
                    int end   = s.getEndChapter()   != null ? s.getEndChapter()   : start;
                    return Math.abs(end - start) + 1;
                })
                .sum();
        stats.setTotalChaptersRead((int) totalChapters);
        stats.setAverageReadingSpeedWpm(0);

        LocalDateTime prevSince = since.minusDays(period);
        List<ReadingSession> prevSessions =
                sessionMapper.findUserSessionsBetween(user.getId(), prevSince, since);

        int currMinutes = totalSeconds / 60;
        int prevMinutes = prevSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0)
                .sum();

        stats.setReadingTimeTrend(buildTrend(currMinutes, prevMinutes, "waktu baca"));
        stats.setCompletionTrend(buildTrend(
                (int) totalBooks,
                (int) prevSessions.stream().map(ReadingSession::getBookId).distinct().count(),
                "buku selesai"));
        stats.setSpeedTrend(new TrendData("neutral", 0.0, "Data WPM tidak tersedia untuk EPUB"));
        stats.setGenreBreakdown(buildGenreBreakdown(user.getId(), sessions));
        stats.setPeakReadingTimes(buildPeakReadingTimes(sessions));

        return new DataResponse<>(SUCCESS, "Statistics retrieved", HttpStatus.OK.value(), stats);
    }

    private List<GenreBreakdownItem> buildGenreBreakdown(Long userId, List<ReadingSession> sessions) {
        Map<String, List<ReadingSession>> byGenre = new HashMap<>();
        for (ReadingSession session : sessions) {
            Book book = bookMapper.findById(session.getBookId());
            if (book == null || book.getCategory() == null) continue;
            byGenre.computeIfAbsent(book.getCategory(), k -> new ArrayList<>()).add(session);
        }

        int totalSessions = sessions.size();
        return byGenre.entrySet().stream()
                .map(entry -> {
                    List<ReadingSession> gs = entry.getValue();
                    int minutesSpent = gs.stream()
                            .mapToInt(s -> s.getTotalDurationSeconds() != null
                                    ? s.getTotalDurationSeconds() / 60 : 0)
                            .sum();
                    GenreBreakdownItem item = new GenreBreakdownItem();
                    item.setGenreName(entry.getKey());
                    item.setBooksRead((int) gs.stream().map(ReadingSession::getBookId).distinct().count());
                    item.setMinutesSpent(minutesSpent);
                    item.setPercentage(totalSessions > 0 ? (gs.size() * 100.0) / totalSessions : 0);
                    item.setAverageRating(0.0);
                    return item;
                })
                .sorted(Comparator.comparing(GenreBreakdownItem::getMinutesSpent, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private List<PeakReadingTimeItem> buildPeakReadingTimes(List<ReadingSession> sessions) {
        Map<Integer, Integer> minutesByHour = new HashMap<>();
        for (ReadingSession session : sessions) {
            if (session.getStartedAt() == null) continue;
            int hour    = session.getStartedAt().getHour();
            int minutes = session.getTotalDurationSeconds() != null
                    ? session.getTotalDurationSeconds() / 60 : 0;
            minutesByHour.merge(hour, minutes, Integer::sum);
        }

        int totalMinutes = minutesByHour.values().stream().mapToInt(Integer::intValue).sum();
        return minutesByHour.entrySet().stream()
                .map(e -> {
                    PeakReadingTimeItem item = new PeakReadingTimeItem();
                    item.setHour(e.getKey());
                    item.setMinutesRead(e.getValue());
                    item.setPercentage(totalMinutes > 0 ? (e.getValue() * 100.0) / totalMinutes : 0.0);
                    return item;
                })
                .sorted(Comparator.comparing(PeakReadingTimeItem::getHour))
                .collect(Collectors.toList());
    }

    // ═════════════════════════════════════════════════════════
    // CALENDAR
    // ═════════════════════════════════════════════════════════

    @Override
    public DataResponse<CalendarResponse> getCalendar(int year, int month) {
        User user = getCurrentUser();

        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end   = start.plusMonths(1);

        List<ReadingSession> sessions =
                sessionMapper.findUserSessionsBetween(user.getId(), start, end);

        Map<Integer, List<ReadingSession>> byDay = sessions.stream()
                .filter(s -> s.getStartedAt() != null)
                .collect(Collectors.groupingBy(s -> s.getStartedAt().getDayOfMonth()));

        List<CalendarDayResponse> days = byDay.entrySet().stream()
                .map(entry -> {
                    List<ReadingSession> daySessions = entry.getValue();
                    int minutesRead = daySessions.stream()
                            .mapToInt(s -> s.getTotalDurationSeconds() != null
                                    ? s.getTotalDurationSeconds() / 60 : 0)
                            .sum();

                    // Ambil buku unik beserta cover-nya
                    List<CalendarBookEntry> bookEntries = daySessions.stream()
                            .map(s -> bookMapper.findById(s.getBookId()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toMap(
                                    Book::getId,
                                    b -> b,
                                    (a, b) -> a,
                                    LinkedHashMap::new
                            ))
                            .values().stream()
                            .map(b -> {
                                CalendarBookEntry entry2 = new CalendarBookEntry();
                                entry2.setTitle(b.getTitle());
                                // coverImageUrl diteruskan agar frontend bisa tampilkan thumbnail
                                entry2.setCoverImageUrl(b.getCoverImageUrl());
                                return entry2;
                            })
                            .collect(Collectors.toList());

                    CalendarDayResponse dayResponse = new CalendarDayResponse();
                    dayResponse.setDay(entry.getKey());
                    dayResponse.setMinutesRead(minutesRead);
                    dayResponse.setPagesRead(minutesRead > 0 ? minutesRead * 2 : 0);
                    dayResponse.setBooks(bookEntries);
                    return dayResponse;
                })
                .sorted(Comparator.comparing(CalendarDayResponse::getDay))
                .collect(Collectors.toList());

        CalendarResponse calResponse = new CalendarResponse();
        calResponse.setDays(days);
        calResponse.setTotalMinutes(days.stream().mapToInt(CalendarDayResponse::getMinutesRead).sum());
        calResponse.setTotalPages(days.stream().mapToInt(CalendarDayResponse::getPagesRead).sum());
        calResponse.setActiveDays(days.size());

        return new DataResponse<>(SUCCESS, "Calendar retrieved", HttpStatus.OK.value(), calResponse);
    }

    // ═════════════════════════════════════════════════════════
    // GOALS & ACHIEVEMENTS (placeholder)
    // ═════════════════════════════════════════════════════════

//    @Override
//    public DataResponse<GoalsResponse> getGoals() {
//        GoalsSummary summary = new GoalsSummary();
//        summary.setTotal(0); summary.setCompleted(0);
//        summary.setActive(0); summary.setThisMonth(0);
//        GoalsResponse response = new GoalsResponse();
//        response.setSummary(summary); response.setActive(List.of()); response.setCompleted(List.of());
//        return new DataResponse<>(SUCCESS, "Goals retrieved", HttpStatus.OK.value(), response);
//    }

    @Override
    public DataResponse<AchievementsResponse> getAchievements() {
        AchievementsResponse response = new AchievementsResponse();
        response.setList(List.of()); response.setTotal(0);
        response.setUnlocked(0); response.setCategories(List.of());
        return new DataResponse<>(SUCCESS, "Achievements retrieved", HttpStatus.OK.value(), response);
    }

    // ═════════════════════════════════════════════════════════
    // USER REVIEWS
    // ═════════════════════════════════════════════════════════

    @Override
    public DatatableResponse<UserReviewItemResponse> getUserReviews(int page, int limit) {
        User user  = getCurrentUser();
        int offset = (page - 1) * limit;

        List<ChapterReview> reviews = reviewMapper.findByUser(user.getId(), offset, limit);
        int total = reviewMapper.countByUser(user.getId());

        List<UserReviewItemResponse> items = reviews.stream()
                .map(r -> {
                    Book book = bookMapper.findById(r.getBookId());
                    UserReviewItemResponse item = new UserReviewItemResponse();
                    item.setReviewId(r.getId());
                    item.setBookId(r.getBookId());
                    item.setBookTitle(book != null ? book.getTitle() : "");
                    item.setBookSlug(book != null ? book.getSlug() : "");
                    item.setBookCover(book != null ? book.getCoverImageUrl() : null);
                    item.setReviewContent(r.getContent());
                    item.setCreatedAt(r.getCreatedAt());
                    return item;
                })
                .toList();

        PageDataResponse<UserReviewItemResponse> pageData =
                new PageDataResponse<>(page, limit, total, items);
        return new DatatableResponse<>(SUCCESS, "Reviews retrieved", HttpStatus.OK.value(), pageData);
    }

    // ═════════════════════════════════════════════════════════
    // RECOMMENDATIONS
    // ═════════════════════════════════════════════════════════

    @Override
    public DataResponse<List<BookRecommendationResponse>> getPersonalizedRecommendations(int limit) {
        User user = getCurrentUser();

        List<ReadingSession> sessions = sessionMapper.findAllUserSessions(user.getId());

        List<String> favoriteGenres = sessions.stream()
                .map(s -> bookMapper.findById(s.getBookId()))
                .filter(Objects::nonNull).map(Book::getCategory).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey).limit(3).toList();

        return new DataResponse<>(SUCCESS, "Recommendations retrieved", HttpStatus.OK.value(),
                bookMapper.getRecommendations(user.getId(), favoriteGenres, limit));
    }

    // ═════════════════════════════════════════════════════════
    // QUICK STATS
    // ═════════════════════════════════════════════════════════

    @Override
    public DataResponse<QuickStatsResponse> getQuickStats() {
        User user = getCurrentUser();

        List<ReadingSession> allSessions = sessionMapper.findAllUserSessions(user.getId());

        QuickStatsResponse stats = new QuickStatsResponse();
        stats.setTotalBooks((int) allSessions.stream().map(ReadingSession::getBookId).distinct().count());
        stats.setCurrentStreak(calculateCurrentStreak(allSessions));

        List<ReadingProgress> allProgress = readingProgressMapper.findAllByUser(user.getId());
        stats.setCompletedBooks((int) allProgress.stream()
                .filter(p -> p.getPercentageCompleted() != null
                        && p.getPercentageCompleted().doubleValue() >= 95.0)
                .count());

        int todayMinutes = getTodayMinutes(user.getId());
        stats.setHasActivityToday(todayMinutes > 0);

        int totalSeconds = allSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0)
                .sum();
        stats.setReadingTime(String.format("%dh %dm", totalSeconds / 3600, (totalSeconds % 3600) / 60));

        return new DataResponse<>(SUCCESS, "Quick stats retrieved", HttpStatus.OK.value(), stats);
    }

    private int getTodayMinutes(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return sessionMapper.findUserSessionsBetween(userId, startOfDay, startOfDay.plusDays(1))
                .stream().mapToInt(s -> s.getTotalDurationSeconds() != null
                        ? s.getTotalDurationSeconds() / 60 : 0).sum();
    }

    // ═════════════════════════════════════════════════════════
    // EXPORT
    // ═════════════════════════════════════════════════════════

    @Override
    public DataResponse<ExportJobResponse> exportUserReadingData(String format) {
        User user = getCurrentUser();
        ExportJobResponse job = new ExportJobResponse();
        job.setExportId(null); job.setStatus("PENDING");
        job.setFormat(format); job.setRequestedAt(LocalDateTime.now());
        return new DataResponse<>(SUCCESS, "Export job queued", HttpStatus.ACCEPTED.value(), job);
    }

    // ═════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════

    private String getAuthorName(Long bookId) {
        try {
            List<String> names = bookMapper.findAuthorNamesByBookId(bookId);
            return (names != null && !names.isEmpty()) ? names.get(0) : "";
        } catch (Exception e) {
            log.warn("Could not fetch author for bookId={}: {}", bookId, e.getMessage());
            return "";
        }
    }

    private int calculateCurrentStreak(List<ReadingSession> sessions) {
        if (sessions.isEmpty()) return 0;
        List<LocalDateTime> dates = sessions.stream()
                .map(ReadingSession::getStartedAt).filter(Objects::nonNull)
                .map(dt -> dt.toLocalDate().atStartOfDay()).distinct()
                .sorted(Comparator.reverseOrder()).toList();

        int streak    = 0;
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).toLocalDate().atStartOfDay();
        for (LocalDateTime date : dates) {
            if (!date.isAfter(LocalDateTime.now().toLocalDate().atStartOfDay())
                    && (date.isEqual(yesterday) || date.isAfter(yesterday))) {
                streak++; yesterday = date.minusDays(1);
            } else break;
        }
        return streak;
    }

    private int calculateLongestStreak(List<ReadingSession> sessions) {
        if (sessions.isEmpty()) return 0;
        List<LocalDateTime> dates = sessions.stream()
                .map(ReadingSession::getStartedAt).filter(Objects::nonNull)
                .map(dt -> dt.toLocalDate().atStartOfDay()).distinct().sorted().toList();

        int maxStreak = 1, current = 1;
        for (int i = 1; i < dates.size(); i++) {
            long daysBetween = Duration.between(dates.get(i - 1), dates.get(i)).toDays();
            if (daysBetween == 1) { maxStreak = Math.max(maxStreak, ++current); }
            else                  { current = 1; }
        }
        return maxStreak;
    }

    private TrendData buildTrend(int current, int previous, String label) {
        if (previous == 0) return new TrendData("neutral", 0.0, "Belum ada data periode sebelumnya");
        double changePct = ((current - previous) * 100.0) / previous;
        String direction = changePct > 5 ? "up" : changePct < -5 ? "down" : "neutral";
        String interpretation = direction.equals("up")
                ? String.format("%s meningkat %.1f%% dari periode sebelumnya", label, changePct)
                : direction.equals("down")
                ? String.format("%s menurun %.1f%% dari periode sebelumnya", label, Math.abs(changePct))
                : String.format("%s stabil dari periode sebelumnya", label);
        return new TrendData(direction, changePct, interpretation);
    }

    private String getReadingTimeLabel(Integer hour) {
        if (hour == null)              return "Tidak diketahui";
        if (hour >= 5  && hour < 12)  return "Pagi";
        if (hour >= 12 && hour < 17)  return "Siang";
        if (hour >= 17 && hour < 21)  return "Sore";
        return "Malam";
    }

    private String getReadingPaceLabel(double chaptersPerDay) {
        if (chaptersPerDay < 0.5) return "Santai";
        if (chaptersPerDay < 2)   return "Sedang";
        return "Cepat";
    }

    private int safeInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString()); } catch (Exception e) { return 0; }
    }
}