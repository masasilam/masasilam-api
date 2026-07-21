package com.masasilam.app.service.common.impl;

import com.masasilam.app.exception.custom.UnauthorizedException;
import com.masasilam.app.mapper.annotation.EpubAnnotationMapper;
import com.masasilam.app.mapper.book.BookMapper;
import com.masasilam.app.mapper.book.EpubBookmarkMapper;
import com.masasilam.app.mapper.chapter.ChapterProgressMapper;
import com.masasilam.app.mapper.chapter.ChapterRatingMapper;
import com.masasilam.app.mapper.chapter.ChapterReviewMapper;
import com.masasilam.app.mapper.chapter.SearchMapper;
import com.masasilam.app.mapper.reading.ReadingActivityMapper;
import com.masasilam.app.mapper.reading.ReadingProgressMapper;
import com.masasilam.app.mapper.reading.ReadingSessionMapper;
import com.masasilam.app.mapper.reading.UserReadingPatternMapper;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.mapper.zine.ZineMapper;
import com.masasilam.app.mapper.zine.ZineReadingProgressMapper;
import com.masasilam.app.mapper.zine.ZineReadingSessionMapper;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.ZineDashboardDTOs.ZineInProgressResponse;
import com.masasilam.app.model.entity.*;
import com.masasilam.app.service.common.DashboardService;
import com.masasilam.app.util.interceptor.HeaderHolder;
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
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final EpubAnnotationMapper epubAnnotationMapper;
    private final EpubBookmarkMapper epubBookmarkMapper;
    private final ReadingSessionMapper sessionMapper;
    private final ReadingProgressMapper readingProgressMapper;
    private final ChapterProgressMapper chapterProgressMapper;
    private final ReadingActivityMapper activityMapper;
    private final UserReadingPatternMapper patternMapper;
    private final ChapterRatingMapper ratingMapper;
    private final ChapterReviewMapper reviewMapper;
    private final SearchMapper searchMapper;
    private final ZineMapper zineMapper;
    private final ZineReadingSessionMapper zineSessionMapper;
    private final ZineReadingProgressMapper zineProgressMapper;
    private final HeaderHolder headerHolder;

    private static final String SESSION_TYPE_EPUB = "EPUB";
    private static final String SESSION_TYPE_CHAPTER = "CHAPTER";
    private static final String SUCCESS = "Success";
    private static final String READING = "reading";
    private static final String COMPLETED = "completed";
    private static final String BOOKMARKED = "bookmarked";
    private static final String NOT_STARTED = "not_started";
    private static final String HIGHLIGHT = "highlight";
    private static final String NOTE = "note";
    private static final String BOOKMARK = "bookmark";
    private static final String ALL = "all";
    private static final String BOOK = "book";
    private static final String PROGRESS = "progress";
    private static final String TITLE = "title";
    private static final String UP = "up";
    private static final String DOWN = "down";
    private static final String NEUTRAL = "neutral";
    private static final String PAGI = "Pagi";
    private static final String SIANG = "Siang";
    private static final String SORE = "Sore";
    private static final String MALAM = "Malam";
    private static final String SANTAI = "Santai";
    private static final String SEDANG = "Sedang";
    private static final String CEPAT = "Cepat";

    private int parseCfiChapter(String cfi) {
        if (cfi == null || cfi.isBlank()) return -1;
        try {
            String base = cfi.split("!")[0];
            String inner = base.replace("epubcfi(", "");
            String[] parts = inner.split("/");
            if (parts.length < 3) return -1;
            int spineStep = Integer.parseInt(parts[2]);
            if (spineStep < 2) return -1;
            return spineStep / 2;
        } catch (Exception e) {
            log.debug("parseCfiChapter failed for cfi={}: {}", cfi, e.getMessage());
            return -1;
        }
    }

    private int resolveChapter(ReadingProgress progress, ReadingSession session) {
        String cfi = progress != null ? progress.getCurrentPosition() : null;
        int cfiChapter = parseCfiChapter(cfi);
        if (cfiChapter > 0) return cfiChapter;
        if (progress != null && progress.getCurrentPage() != null && progress.getCurrentPage() > 0)
            return progress.getCurrentPage();
        return session != null && session.getStartChapter() != null ? session.getStartChapter() : 0;
    }

    private int resolveZineChapter(ZineReadingProgress progress, ZineReadingSession session) {
        String cfi = progress != null ? progress.getCurrentPosition() : null;
        int cfiChapter = parseCfiChapter(cfi);
        if (cfiChapter > 0) return cfiChapter;
        if (progress != null && progress.getCurrentPage() != null && progress.getCurrentPage() > 0)
            return progress.getCurrentPage();
        return session != null && session.getStartChapter() != null ? session.getStartChapter() : 0;
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
                    && (session.getEndChapter() == null || session.getEndChapter() == 0)
                    && (session.getChaptersRead() == null || session.getChaptersRead() == 0);
        }
        return false;
    }

    @Override
    public DataResponse<DashboardMainResponse> getMainDashboard() {
        User user = getCurrentUser();

        DashboardMainResponse response = new DashboardMainResponse();
        response.setOverviewStats(buildOverviewStats(user.getId()));
        response.setBooksInProgress(buildBooksInProgress(user.getId()));
        response.setZinesInProgress(buildZinesInProgress(user.getId()));
        response.setReadingPattern(buildReadingPatternSummary(user.getId()));
        response.setRecentlyRead(buildRecentlyRead(user.getId()));
        response.setAnnotationsSummary(buildAnnotationsSummary(user.getId()));
        response.setRecentAchievements(List.of());

        return new DataResponse<>(SUCCESS, "Dashboard retrieved", HttpStatus.OK.value(), response);
    }

    private OverviewStats buildOverviewStats(Long userId) {
        OverviewStats stats = new OverviewStats();

        List<ReadingSession> bookSessions = sessionMapper.findAllUserSessions(userId);
        List<ZineReadingSession> zineSessions = zineSessionMapper.findAllUserSessions(userId);

        long totalBooks = bookSessions.stream()
                .map(ReadingSession::getBookId).filter(Objects::nonNull).distinct().count();
        long totalZines = zineSessions.stream()
                .map(ZineReadingSession::getZineId).filter(Objects::nonNull).distinct().count();
        stats.setTotalBooks((int) (totalBooks + totalZines));

        List<ReadingProgress> allBookProgress = readingProgressMapper.findAllByUser(userId);
        List<ZineReadingProgress> allZineProgress = zineProgressMapper.findAllByUser(userId);
        long completedBooks = allBookProgress.stream()
                .filter(p -> p.getPercentageCompleted() != null && p.getPercentageCompleted().doubleValue() >= 95.0)
                .count();
        long completedZines = allZineProgress.stream()
                .filter(p -> p.getPercentageCompleted() != null && p.getPercentageCompleted().doubleValue() >= 95.0)
                .count();
        stats.setBooksCompleted((int) (completedBooks + completedZines));

        int bookSeconds = bookSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0).sum();
        int zineSeconds = zineSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0).sum();
        stats.setTotalReadingTimeHours((bookSeconds + zineSeconds) / 3600);

        List<LocalDateTime> allDates = new ArrayList<>();
        bookSessions.stream().map(ReadingSession::getStartedAt).filter(Objects::nonNull).forEach(allDates::add);
        zineSessions.stream().map(ZineReadingSession::getStartedAt).filter(Objects::nonNull).forEach(allDates::add);
        stats.setCurrentStreak(calculateCurrentStreak(allDates));
        stats.setLongestStreak(calculateLongestStreak(allDates));

        List<Map<String, Object>> ratingData = ratingMapper.findAllUserRatings(userId);
        stats.setAverageRating(ratingData.isEmpty() ? 0.0
                : ratingData.stream().mapToInt(r -> safeInt(r.get("rating"))).average().orElse(0.0));

        List<Double> allPcts = new ArrayList<>();
        allBookProgress.forEach(p -> allPcts.add(p.getPercentageCompleted() != null
                ? p.getPercentageCompleted().doubleValue() : 0.0));
        allZineProgress.forEach(p -> allPcts.add(p.getPercentageCompleted() != null
                ? p.getPercentageCompleted().doubleValue() : 0.0));
        stats.setCompletionRate(allPcts.isEmpty() ? 0.0
                : allPcts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));

        return stats;
    }

    private List<BookInProgressResponse> buildBooksInProgress(Long userId) {
        List<ReadingSession> latestSessions = sessionMapper.findLatestSessionPerBook(userId);

        return latestSessions.stream()
                .map(session -> {
                    Book book = bookMapper.findById(session.getBookId());
                    if (book == null) return null;

                    ReadingProgress progress = readingProgressMapper.findByUserAndBook(userId, book.getId());
                    double pct = progress != null && progress.getPercentageCompleted() != null
                            ? progress.getPercentageCompleted().doubleValue() : 0.0;
                    if (pct >= 95.0) return null;

                    int displayChapter = resolveChapter(progress, session);

                    BookInProgressResponse r = new BookInProgressResponse();
                    r.setBookId(book.getId());
                    r.setBookSlug(book.getSlug());
                    r.setBookTitle(book.getTitle());
                    r.setAuthorName(getBookAuthorName(book.getId()));
                    r.setCoverImageUrl(book.getCoverImageUrl());
                    r.setProgressPercentage(pct);
                    r.setLastReadAt(session.getStartedAt());
                    r.setCurrentChapter(displayChapter);
                    r.setTotalChapters(progress != null && progress.getTotalPages() != null
                            ? progress.getTotalPages() : 0);
                    r.setLastCfi(progress != null ? progress.getCurrentPosition() : null);
                    return r;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BookInProgressResponse::getLastReadAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<ZineInProgressResponse> buildZinesInProgress(Long userId) {
        List<ZineReadingSession> latestSessions = zineSessionMapper.findLatestSessionPerZine(userId);

        return latestSessions.stream()
                .map(session -> {
                    Zine zine = zineMapper.findById(session.getZineId());
                    if (zine == null) return null;

                    ZineReadingProgress progress = zineProgressMapper.findByUserAndZine(userId, zine.getId());
                    double pct = progress != null && progress.getPercentageCompleted() != null
                            ? progress.getPercentageCompleted().doubleValue() : 0.0;
                    if (pct >= 95.0) return null;

                    int displayChapter = resolveZineChapter(progress, session);

                    ZineInProgressResponse r = new ZineInProgressResponse();
                    r.setZineId(zine.getId());
                    r.setZineSlug(zine.getSlug());
                    r.setZineTitle(zine.getTitle());
                    r.setAuthorName(getZineAuthorName(zine.getId()));
                    r.setCoverImageUrl(zine.getCoverImageUrl());
                    r.setProgressPercentage(pct);
                    r.setLastReadAt(session.getStartedAt());
                    r.setCurrentChapter(displayChapter);
                    r.setTotalChapters(progress != null && progress.getTotalPages() != null
                            ? progress.getTotalPages() : 0);
                    r.setLastCfi(progress != null ? progress.getCurrentPosition() : null);
                    return r;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ZineInProgressResponse::getLastReadAt,
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
            summary.setEstimatedReadingSpeedWpm(0.0);
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

        int totalMinutes = allSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0)
                .sum() / 60;

        long totalChaptersNonEpub = allSessions.stream().filter(s -> !isEpubSession(s))
                .mapToInt(s -> {
                    int start = s.getStartChapter() != null ? s.getStartChapter() : 0;
                    int end = s.getEndChapter() != null ? s.getEndChapter() : start;
                    return Math.max(0, Math.abs(end - start) + 1);
                }).sum();
        long totalEpubChapters = allSessions.stream().filter(this::isEpubSession)
                .mapToInt(s -> s.getChaptersRead() != null ? s.getChaptersRead() : 0).sum();
        long totalBab = totalChaptersNonEpub + totalEpubChapters;

        if (totalMinutes > 0 && totalBab > 0) {
            double estimatedWpm = Math.max(80, Math.min(600, (totalBab * 3000.0) / totalMinutes));
            summary.setEstimatedReadingSpeedWpm(estimatedWpm);
        } else {
            summary.setEstimatedReadingSpeedWpm(0.0);
        }

        int totalSeconds = allSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0).sum();
        long totalDays = ChronoUnit.DAYS.between(
                allSessions.stream().map(ReadingSession::getStartedAt).filter(Objects::nonNull)
                        .min(Comparator.naturalOrder()).orElse(LocalDateTime.now()),
                LocalDateTime.now());
        double chaptersPerDay = totalDays > 0 ? (totalSeconds / 300.0) / totalDays : 0;
        summary.setReadingPace(getReadingPaceLabel(chaptersPerDay));

        return summary;
    }

    private List<RecentlyReadResponse> buildRecentlyRead(Long userId) {
        List<ReadingSession> recentBookSessions = sessionMapper.findRecentUserSessions(userId, 6);
        List<ZineReadingSession> recentZineSessions = zineSessionMapper.findRecentUserSessions(userId, 6);

        List<RecentlyReadResponse> combined = new ArrayList<>();

        Map<Long, ReadingSession> latestBookPerBook = new LinkedHashMap<>();
        for (ReadingSession session : recentBookSessions) {
            latestBookPerBook.putIfAbsent(session.getBookId(), session);
        }
        for (ReadingSession session : latestBookPerBook.values()) {
            Book book = bookMapper.findById(session.getBookId());
            if (book == null) continue;

            ReadingProgress progress = readingProgressMapper.findByUserAndBook(userId, book.getId());
            double pct = progress != null && progress.getPercentageCompleted() != null
                    ? progress.getPercentageCompleted().doubleValue() : 0.0;
            int displayChapter = resolveChapter(progress, session);

            RecentlyReadResponse r = new RecentlyReadResponse();
            r.setBookId(book.getId());
            r.setBookSlug(book.getSlug());
            r.setBookTitle(book.getTitle());
            r.setAuthorName(getBookAuthorName(book.getId()));
            r.setCoverImageUrl(book.getCoverImageUrl());
            r.setLastReadAt(session.getStartedAt());
            r.setActivityType(pct >= 95.0 ? COMPLETED : pct > 0 ? READING : NOT_STARTED);
            r.setCurrentChapter(displayChapter);
            r.setTotalChapters(progress != null && progress.getTotalPages() != null ? progress.getTotalPages() : 0);
            r.setProgressPercentage(pct);
            r.setLastCfi(progress != null ? progress.getCurrentPosition() : null);
            combined.add(r);
        }

        Map<Long, ZineReadingSession> latestZinePerZine = new LinkedHashMap<>();
        for (ZineReadingSession session : recentZineSessions) {
            latestZinePerZine.putIfAbsent(session.getZineId(), session);
        }
        for (ZineReadingSession session : latestZinePerZine.values()) {
            Zine zine = zineMapper.findById(session.getZineId());
            if (zine == null) continue;

            ZineReadingProgress progress = zineProgressMapper.findByUserAndZine(userId, zine.getId());
            double pct = progress != null && progress.getPercentageCompleted() != null
                    ? progress.getPercentageCompleted().doubleValue() : 0.0;
            int displayChapter = resolveZineChapter(progress, session);

            RecentlyReadResponse r = new RecentlyReadResponse();
            r.setBookId(zine.getId());
            r.setBookSlug("zines/" + zine.getSlug());
            r.setBookTitle("[Zine] " + zine.getTitle());
            r.setAuthorName(getZineAuthorName(zine.getId()));
            r.setCoverImageUrl(zine.getCoverImageUrl());
            r.setLastReadAt(session.getStartedAt());
            r.setActivityType(pct >= 95.0 ? COMPLETED : pct > 0 ? READING : NOT_STARTED);
            r.setCurrentChapter(displayChapter);
            r.setTotalChapters(progress != null && progress.getTotalPages() != null ? progress.getTotalPages() : 0);
            r.setProgressPercentage(pct);
            r.setLastCfi(progress != null ? progress.getCurrentPosition() : null);
            combined.add(r);
        }

        return combined.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecentlyReadResponse::getLastReadAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
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

    @Override
    public DataResponse<AnnotationsPageResponse> getAnnotations(String type, int page, int limit, String sortBy) {
        User user = getCurrentUser();
        int offset = (page - 1) * limit;

        List<AnnotationItemResponse> items = new ArrayList<>();
        int totalCount = 0;

        if (ALL.equals(type) || HIGHLIGHT.equals(type) || NOTE.equals(type)) {
            List<EpubAnnotation> epubAnnotations = epubAnnotationMapper.findByUserPaged(user.getId(), offset, limit);

            for (EpubAnnotation ann : epubAnnotations) {
                boolean hasNote = ann.getNote() != null && !ann.getNote().isBlank();
                if (HIGHLIGHT.equals(type) && hasNote) continue;
                if (NOTE.equals(type) && !hasNote) continue;

                Book book = bookMapper.findById(ann.getBookId());
                AnnotationItemResponse item = new AnnotationItemResponse();
                item.setId(ann.getId());
                item.setType(hasNote ? NOTE : HIGHLIGHT);
                item.setContent(ann.getSelectedText());
                item.setCfi(ann.getCfi());
                item.setCreatedAt(ann.getCreatedAt());

                if (book != null) {
                    item.setBookId(book.getId());
                    item.setBookSlug(book.getSlug());
                    item.setBookTitle(book.getTitle());
                    item.setBookCover(book.getCoverImageUrl());
                } else {
                    Zine zine = zineMapper.findById(ann.getBookId());
                    if (zine != null) {
                        item.setBookId(zine.getId());
                        item.setBookSlug("zines/" + zine.getSlug());
                        item.setBookTitle("[Zine] " + zine.getTitle());
                        item.setBookCover(zine.getCoverImageUrl());
                    }
                }
                items.add(item);
            }

            totalCount += HIGHLIGHT.equals(type)
                    ? epubAnnotationMapper.countHighlightsByUser(user.getId())
                    : NOTE.equals(type)
                    ? epubAnnotationMapper.countNotesByUser(user.getId())
                    : epubAnnotationMapper.countByUser(user.getId());
        }

        if (ALL.equals(type) || BOOKMARK.equals(type)) {
            List<EpubBookmark> epubBookmarks = epubBookmarkMapper.findByUser(user.getId(), offset, limit);

            for (EpubBookmark bm : epubBookmarks) {
                Book book = bookMapper.findById(bm.getBookId());
                AnnotationItemResponse item = new AnnotationItemResponse();
                item.setId(bm.getId());
                item.setType(BOOKMARK);
                item.setContent(bm.getLabel());
                item.setCfi(bm.getCfi());
                item.setCreatedAt(bm.getCreatedAt());

                if (book != null) {
                    item.setBookId(book.getId());
                    item.setBookSlug(book.getSlug());
                    item.setBookTitle(book.getTitle());
                    item.setBookCover(book.getCoverImageUrl());
                } else {
                    Zine zine = zineMapper.findById(bm.getBookId());
                    if (zine != null) {
                        item.setBookId(zine.getId());
                        item.setBookSlug("zines/" + zine.getSlug());
                        item.setBookTitle("[Zine] " + zine.getTitle());
                        item.setBookCover(zine.getCoverImageUrl());
                    }
                }
                items.add(item);
            }
            totalCount += epubBookmarkMapper.countByUser(user.getId());
        }

        Comparator<AnnotationItemResponse> comparator = BOOK.equals(sortBy)
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

    @Override
    public DataResponse<LibraryPageResponse> getLibrary(String filter, int page, int limit, String sortBy) {
        User user = getCurrentUser();
        List<ReadingSession> allSessions = sessionMapper.findAllUserSessions(user.getId());

        Map<Long, ReadingSession> latestPerBook = new LinkedHashMap<>();
        for (ReadingSession session : allSessions) {
            latestPerBook.merge(session.getBookId(), session, (existing, incoming) -> {
                if (incoming.getStartedAt() != null && existing.getStartedAt() != null)
                    return incoming.getStartedAt().isAfter(existing.getStartedAt()) ? incoming : existing;
                return existing;
            });
        }

        List<LibraryBookResponse> books = latestPerBook.entrySet().stream()
                .map(entry -> {
                    Book book = bookMapper.findById(entry.getKey());
                    if (book == null) return null;

                    ReadingProgress progress = readingProgressMapper.findByUserAndBook(user.getId(), book.getId());
                    double pct = progress != null && progress.getPercentageCompleted() != null
                            ? progress.getPercentageCompleted().doubleValue() : 0.0;
                    String status = pct >= 95.0 ? COMPLETED : pct > 0 ? READING : NOT_STARTED;

                    if (READING.equals(filter) && !READING.equals(status)) return null;
                    if (COMPLETED.equals(filter) && !COMPLETED.equals(status)) return null;
                    if (BOOKMARKED.equals(filter)
                            && epubBookmarkMapper.countByUserAndBook(user.getId(), book.getId()) == 0)
                        return null;

                    ReadingSession session = entry.getValue();
                    int displayChapter = resolveChapter(progress, session);

                    LibraryBookResponse r = new LibraryBookResponse();
                    r.setBookId(book.getId());
                    r.setBookSlug(book.getSlug());
                    r.setBookTitle(book.getTitle());
                    r.setAuthorName(getBookAuthorName(book.getId()));
                    r.setCoverImageUrl(book.getCoverImageUrl());
                    r.setProgressPercentage(pct);
                    r.setReadingStatus(status);
                    r.setLastReadAt(session.getStartedAt());
                    r.setBookmarkCount(epubBookmarkMapper.countByUserAndBook(user.getId(), book.getId()));
                    r.setHighlightCount(epubAnnotationMapper.countByUserAndBook(user.getId(), book.getId()));
                    r.setNoteCount(0);
                    r.setCurrentChapter(displayChapter);
                    r.setTotalChapters(progress != null && progress.getTotalPages() != null ? progress.getTotalPages() : 0);
                    r.setLastCfi(progress != null ? progress.getCurrentPosition() : null);
                    return r;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Comparator<LibraryBookResponse> comparator = switch (sortBy) {
            case PROGRESS ->
                    Comparator.comparing(LibraryBookResponse::getProgressPercentage, Comparator.reverseOrder());
            case TITLE ->
                    Comparator.comparing(LibraryBookResponse::getBookTitle, Comparator.nullsLast(Comparator.naturalOrder()));
            default ->
                    Comparator.comparing(LibraryBookResponse::getLastReadAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        books.sort(comparator);

        int total = books.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);

        LibraryPageResponse pageResponse = new LibraryPageResponse();
        pageResponse.setItems(books.subList(fromIndex, toIndex));
        pageResponse.setTotalData(total);
        pageResponse.setPage(page);
        pageResponse.setLimit(limit);

        return new DataResponse<>(SUCCESS, "Library retrieved", HttpStatus.OK.value(), pageResponse);
    }

    @Override
    public DataResponse<ReadingHistoryPageResponse> getReadingHistory(int days, int page, int limit) {
        User user = getCurrentUser();
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<ReadingSession> sessions = sessionMapper.findUserSessionsSince(user.getId(), since);
        sessions.sort(Comparator.comparing(ReadingSession::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<ReadingHistoryItemResponse> items = sessions.stream()
                .map(session -> {
                    Book book = bookMapper.findById(session.getBookId());
                    if (book == null) return null;

                    ReadingProgress progress = readingProgressMapper.findByUserAndBook(user.getId(), book.getId());
                    int lastKnownChapter = resolveChapter(progress, session);

                    int durationSec = session.getTotalDurationSeconds() != null ? session.getTotalDurationSeconds() : 0;
                    String durationStr = formatDuration(durationSec);

                    ReadingHistoryItemResponse item = new ReadingHistoryItemResponse();
                    item.setActivityId(session.getId());
                    item.setActivityType("reading_session");
                    item.setBookId(book.getId());
                    item.setBookSlug(book.getSlug());
                    item.setBookTitle(book.getTitle());
                    item.setAuthorName(getBookAuthorName(book.getId()));
                    item.setBookCover(book.getCoverImageUrl());
                    item.setTimestamp(session.getStartedAt());
                    item.setChapterNumber(lastKnownChapter > 0 ? lastKnownChapter : null);
                    item.setDescription(isEpubSession(session)
                            ? String.format("Membaca EPUB (bab %d) selama %s", lastKnownChapter > 0 ? lastKnownChapter : 0, durationStr)
                            : String.format("Membaca hingga Bab %d selama %s", lastKnownChapter > 0 ? lastKnownChapter : 0, durationStr));
                    item.setProgressPercentage(progress != null && progress.getPercentageCompleted() != null
                            ? progress.getPercentageCompleted().doubleValue() : 0.0);
                    item.setLastCfi(progress != null ? progress.getCurrentPosition() : null);
                    return item;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int total = items.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);

        ReadingHistoryPageResponse pageResponse = new ReadingHistoryPageResponse();
        pageResponse.setList(items.subList(fromIndex, toIndex));
        pageResponse.setTotal(total);
        pageResponse.setPage(page);
        pageResponse.setLimit(limit);

        return new DataResponse<>(SUCCESS, "Reading history retrieved", HttpStatus.OK.value(), pageResponse);
    }

    @Override
    public DataResponse<StatisticsResponse> getStatistics(int period) {
        User user = getCurrentUser();
        LocalDateTime since = LocalDateTime.now().minusDays(period);
        List<ReadingSession> sessions = sessionMapper.findUserSessionsSince(user.getId(), since);

        StatisticsResponse stats = new StatisticsResponse();
        long totalBooks = sessions.stream().map(ReadingSession::getBookId).filter(Objects::nonNull).distinct().count();
        stats.setTotalBooksRead((int) totalBooks);

        int totalSeconds = sessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0).sum();
        stats.setTotalReadingMinutes(totalSeconds / 60);

        long totalChaptersNonEpub = sessions.stream().filter(s -> !isEpubSession(s))
                .mapToInt(s -> {
                    int start = s.getStartChapter() != null ? s.getStartChapter() : 0;
                    int end = s.getEndChapter() != null ? s.getEndChapter() : start;
                    return Math.max(0, Math.abs(end - start) + 1);
                }).sum();
        stats.setTotalChaptersRead((int) totalChaptersNonEpub);

        long totalEpubChapters = sessions.stream().filter(this::isEpubSession)
                .mapToInt(s -> s.getChaptersRead() != null ? s.getChaptersRead() : 0).sum();
        stats.setTotalEpubChaptersRead((int) totalEpubChapters);
        stats.setAverageReadingSpeedWpm(0);

        int totalMinutes = totalSeconds / 60;
        if (totalMinutes > 0) {
            long totalBabSemua = totalChaptersNonEpub + totalEpubChapters;
            if (totalBabSemua > 0) {
                double wpm = Math.max(80, Math.min(600, (totalBabSemua * 3000.0) / totalMinutes));
                stats.setEstimatedReadingSpeedWpm(wpm);
            } else stats.setEstimatedReadingSpeedWpm(0);
        } else stats.setEstimatedReadingSpeedWpm(0);

        LocalDateTime prevSince = since.minusDays(period);
        List<ReadingSession> prevSessions = sessionMapper.findUserSessionsBetween(user.getId(), prevSince, since);

        int currMinutes = totalSeconds / 60;
        int prevMinutes = prevSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0).sum();

        stats.setReadingTimeTrend(buildTrend(currMinutes, prevMinutes, "waktu baca"));
        stats.setCompletionTrend(buildTrend(
                (int) totalBooks,
                (int) prevSessions.stream().map(ReadingSession::getBookId).distinct().count(),
                "buku selesai"));

        double currEstWpm = stats.getEstimatedReadingSpeedWpm();
        if (currEstWpm > 0 && prevMinutes > 0) {
            long prevBab = prevSessions.stream().filter(s -> !isEpubSession(s))
                    .mapToInt(s -> {
                        int start = s.getStartChapter() != null ? s.getStartChapter() : 0;
                        int end = s.getEndChapter() != null ? s.getEndChapter() : start;
                        return Math.max(0, Math.abs(end - start) + 1);
                    }).sum()
                    + prevSessions.stream().filter(this::isEpubSession)
                    .mapToInt(s -> s.getChaptersRead() != null ? s.getChaptersRead() : 0).sum();
            double prevEstWpm = prevBab > 0 ? Math.max(80, Math.min(600, (prevBab * 3000.0) / prevMinutes)) : 0;
            stats.setSpeedTrend(buildTrend((int) currEstWpm, (int) prevEstWpm, "kecepatan baca"));
        } else {
            stats.setSpeedTrend(new TrendData(NEUTRAL, 0.0, "Belum ada data kecepatan"));
        }

        stats.setGenreBreakdown(buildBookGenreBreakdown(user.getId(), sessions));
        stats.setPeakReadingTimes(buildBookPeakReadingTimes(sessions));

        return new DataResponse<>(SUCCESS, "Statistics retrieved", HttpStatus.OK.value(), stats);
    }

    @Override
    public DataResponse<CalendarResponse> getCalendar(int year, int month) {
        User user = getCurrentUser();

        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        List<ReadingSession> bookSessions = sessionMapper.findUserSessionsBetween(user.getId(), start, end);
        List<ZineReadingSession> zineSessions = zineSessionMapper.findUserSessionsBetween(user.getId(), start, end);

        Map<Integer, Integer> minutesByDay = new TreeMap<>();

        for (ReadingSession s : bookSessions) {
            if (s.getStartedAt() == null) continue;
            int day = s.getStartedAt().getDayOfMonth();
            int min = s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0;
            minutesByDay.merge(day, min, Integer::sum);
        }
        for (ZineReadingSession s : zineSessions) {
            if (s.getStartedAt() == null) continue;
            int day = s.getStartedAt().getDayOfMonth();
            int min = s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0;
            minutesByDay.merge(day, min, Integer::sum);
        }

        Map<Integer, Map<Long, Book>> booksByDay = new TreeMap<>();
        for (ReadingSession s : bookSessions) {
            if (s.getStartedAt() == null) continue;
            int day = s.getStartedAt().getDayOfMonth();
            Book book = bookMapper.findById(s.getBookId());
            if (book != null) booksByDay.computeIfAbsent(day, k -> new LinkedHashMap<>())
                    .putIfAbsent(book.getId(), book);
        }
        for (ZineReadingSession s : zineSessions) {
            if (s.getStartedAt() == null) continue;
            int day = s.getStartedAt().getDayOfMonth();
            Zine zine = zineMapper.findById(s.getZineId());
            if (zine != null) {
                Book proxy = new Book();
                proxy.setId(zine.getId());
                proxy.setTitle("[Zine] " + zine.getTitle());
                proxy.setCoverImageUrl(zine.getCoverImageUrl());
                booksByDay.computeIfAbsent(day, k -> new LinkedHashMap<>())
                        .putIfAbsent(-zine.getId(), proxy);
            }
        }

        List<CalendarDayResponse> days = minutesByDay.entrySet().stream()
                .map(entry -> {
                    int dayNum = entry.getKey();
                    int minutes = entry.getValue();

                    List<CalendarBookEntry> bookEntries = booksByDay
                            .getOrDefault(dayNum, Collections.emptyMap())
                            .values().stream()
                            .map(b -> {
                                CalendarBookEntry e = new CalendarBookEntry();
                                e.setTitle(b.getTitle());
                                e.setCoverImageUrl(b.getCoverImageUrl());
                                return e;
                            }).collect(Collectors.toList());

                    CalendarDayResponse dayResponse = new CalendarDayResponse();
                    dayResponse.setDay(dayNum);
                    dayResponse.setMinutesRead(minutes);
                    dayResponse.setPagesRead(minutes > 0 ? minutes * 2 : 0);
                    dayResponse.setBooks(bookEntries);
                    return dayResponse;
                })
                .collect(Collectors.toList());

        CalendarResponse calResponse = new CalendarResponse();
        calResponse.setDays(days);
        calResponse.setTotalMinutes(days.stream().mapToInt(CalendarDayResponse::getMinutesRead).sum());
        calResponse.setTotalPages(days.stream().mapToInt(CalendarDayResponse::getPagesRead).sum());
        calResponse.setActiveDays(days.size());

        return new DataResponse<>(SUCCESS, "Calendar retrieved", HttpStatus.OK.value(), calResponse);
    }

    @Override
    public DataResponse<QuickStatsResponse> getQuickStats() {
        User user = getCurrentUser();

        List<ReadingSession> bookSessions = sessionMapper.findAllUserSessions(user.getId());
        List<ZineReadingSession> zineSessions = zineSessionMapper.findAllUserSessions(user.getId());

        long totalBooks = bookSessions.stream().map(ReadingSession::getBookId).distinct().count();
        long totalZines = zineSessions.stream().map(ZineReadingSession::getZineId).distinct().count();

        List<ReadingProgress> allBookProgress = readingProgressMapper.findAllByUser(user.getId());
        List<ZineReadingProgress> allZineProgress = zineProgressMapper.findAllByUser(user.getId());

        long completedBooks = allBookProgress.stream()
                .filter(p -> p.getPercentageCompleted() != null && p.getPercentageCompleted().doubleValue() >= 95.0)
                .count();
        long completedZines = allZineProgress.stream()
                .filter(p -> p.getPercentageCompleted() != null && p.getPercentageCompleted().doubleValue() >= 95.0)
                .count();

        List<LocalDateTime> allDates = new ArrayList<>();
        bookSessions.stream().map(ReadingSession::getStartedAt).filter(Objects::nonNull).forEach(allDates::add);
        zineSessions.stream().map(ZineReadingSession::getStartedAt).filter(Objects::nonNull).forEach(allDates::add);

        int bookSeconds = bookSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0).sum();
        int zineSeconds = zineSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0).sum();
        int totalSeconds = bookSeconds + zineSeconds;

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        int todayBookMinutes = sessionMapper.findUserSessionsBetween(user.getId(), startOfDay, startOfDay.plusDays(1))
                .stream().mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0).sum();
        int todayZineMinutes = zineSessionMapper.findUserSessionsBetween(user.getId(), startOfDay, startOfDay.plusDays(1))
                .stream().mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0).sum();

        QuickStatsResponse stats = new QuickStatsResponse();
        stats.setTotalBooks((int) (totalBooks + totalZines));
        stats.setCurrentStreak(calculateCurrentStreak(allDates));
        stats.setCompletedBooks((int) (completedBooks + completedZines));
        stats.setHasActivityToday((todayBookMinutes + todayZineMinutes) > 0);
        stats.setReadingTime(String.format("%dh %dm", totalSeconds / 3600, (totalSeconds % 3600) / 60));

        return new DataResponse<>(SUCCESS, "Quick stats retrieved", HttpStatus.OK.value(), stats);
    }

    @Override
    public DataResponse<AchievementsResponse> getAchievements() {
        AchievementsResponse response = new AchievementsResponse();
        response.setList(List.of());
        response.setTotal(0);
        response.setUnlocked(0);
        response.setCategories(List.of());
        return new DataResponse<>(SUCCESS, "Achievements retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    public DatatableResponse<UserReviewItemResponse> getUserReviews(int page, int limit) {
        User user = getCurrentUser();
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
                }).toList();

        return new DatatableResponse<>(SUCCESS, "Reviews retrieved", HttpStatus.OK.value(),
                new PageDataResponse<>(page, limit, total, items));
    }

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

    @Override
    public DataResponse<ExportJobResponse> exportUserReadingData(String format) {
        User user = getCurrentUser();
        ExportJobResponse job = new ExportJobResponse();
        job.setExportId(null);
        job.setStatus("PENDING");
        job.setFormat(format);
        job.setRequestedAt(LocalDateTime.now());
        return new DataResponse<>(SUCCESS, "Export job queued", HttpStatus.ACCEPTED.value(), job);
    }

    private String getBookAuthorName(Long bookId) {
        try {
            List<String> names = bookMapper.findAuthorNamesByBookId(bookId);
            return (names != null && !names.isEmpty()) ? names.get(0) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getZineAuthorName(Long zineId) {
        try {
            List<String> names = zineMapper.findAuthorNamesByZineId(zineId);
            return (names != null && !names.isEmpty()) ? names.get(0) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDuration(int totalSeconds) {
        if (totalSeconds >= 3600)
            return String.format("%d jam %d menit", totalSeconds / 3600, (totalSeconds % 3600) / 60);
        if (totalSeconds >= 60) return String.format("%d menit", totalSeconds / 60);
        return totalSeconds > 0 ? String.format("%d detik", totalSeconds) : "kurang dari 1 menit";
    }

    private int calculateCurrentStreak(List<LocalDateTime> allDates) {
        if (allDates.isEmpty()) return 0;
        List<LocalDateTime> dates = allDates.stream()
                .map(dt -> dt.toLocalDate().atStartOfDay()).distinct()
                .sorted(Comparator.reverseOrder()).toList();
        int streak = 0;
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).toLocalDate().atStartOfDay();
        for (LocalDateTime date : dates) {
            if (!date.isAfter(LocalDateTime.now().toLocalDate().atStartOfDay())
                    && (date.isEqual(yesterday) || date.isAfter(yesterday))) {
                streak++;
                yesterday = date.minusDays(1);
            } else break;
        }
        return streak;
    }

    private int calculateLongestStreak(List<LocalDateTime> allDates) {
        if (allDates.isEmpty()) return 0;
        List<LocalDateTime> dates = allDates.stream()
                .map(dt -> dt.toLocalDate().atStartOfDay()).distinct().sorted().toList();
        int maxStreak = 1, current = 1;
        for (int i = 1; i < dates.size(); i++) {
            long days = Duration.between(dates.get(i - 1), dates.get(i)).toDays();
            if (days == 1) {
                maxStreak = Math.max(maxStreak, ++current);
            } else {
                current = 1;
            }
        }
        return maxStreak;
    }

    private TrendData buildTrend(int current, int previous, String label) {
        if (previous == 0) return new TrendData(NEUTRAL, 0.0, "Belum ada data periode sebelumnya");
        double changePct = ((current - previous) * 100.0) / previous;
        String direction = changePct > 5 ? UP : changePct < -5 ? DOWN : NEUTRAL;
        String interpretation = direction.equals(UP)
                ? String.format("%s meningkat %.1f%% dari periode sebelumnya", label, changePct)
                : direction.equals(DOWN)
                ? String.format("%s menurun %.1f%% dari periode sebelumnya", label, Math.abs(changePct))
                : String.format("%s stabil dari periode sebelumnya", label);
        return new TrendData(direction, changePct, interpretation);
    }

    private List<GenreBreakdownItem> buildBookGenreBreakdown(Long userId, List<ReadingSession> sessions) {
        Map<String, List<ReadingSession>> byGenre = new HashMap<>();
        for (ReadingSession session : sessions) {
            Book book = bookMapper.findById(session.getBookId());
            if (book == null || book.getCategory() == null) continue;
            byGenre.computeIfAbsent(book.getCategory(), k -> new ArrayList<>()).add(session);
        }
        int totalSessions = sessions.size();
        return byGenre.entrySet().stream()
                .map(entry -> {
                    int minutesSpent = entry.getValue().stream()
                            .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0)
                            .sum();
                    GenreBreakdownItem item = new GenreBreakdownItem();
                    item.setGenreName(entry.getKey());
                    item.setBooksRead((int) entry.getValue().stream().map(ReadingSession::getBookId).distinct().count());
                    item.setMinutesSpent(minutesSpent);
                    item.setPercentage(totalSessions > 0 ? (entry.getValue().size() * 100.0) / totalSessions : 0);
                    item.setAverageRating(0.0);
                    return item;
                })
                .sorted(Comparator.comparing(GenreBreakdownItem::getMinutesSpent, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private List<PeakReadingTimeItem> buildBookPeakReadingTimes(List<ReadingSession> sessions) {
        Map<Integer, Integer> minutesByHour = new HashMap<>();
        for (ReadingSession session : sessions) {
            if (session.getStartedAt() == null) continue;
            int hour = session.getStartedAt().getHour();
            int minutes = session.getTotalDurationSeconds() != null ? session.getTotalDurationSeconds() / 60 : 0;
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

    private String getReadingTimeLabel(Integer hour) {
        if (hour == null) return "Tidak diketahui";
        if (hour >= 5 && hour < 12) return PAGI;
        if (hour >= 12 && hour < 17) return SIANG;
        if (hour >= 17 && hour < 21) return SORE;
        return MALAM;
    }

    private String getReadingPaceLabel(double chaptersPerDay) {
        if (chaptersPerDay < 0.5) return SANTAI;
        if (chaptersPerDay < 2) return SEDANG;
        return CEPAT;
    }

    private int safeInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}