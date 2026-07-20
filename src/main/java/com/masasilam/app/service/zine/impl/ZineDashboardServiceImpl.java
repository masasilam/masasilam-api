package com.masasilam.app.service.zine.impl;

import com.masasilam.app.exception.custom.UnauthorizedException;
import com.masasilam.app.mapper.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.ZineDashboardDTOs.*;
import com.masasilam.app.model.entity.*;
import com.masasilam.app.service.zine.ZineDashboardService;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZineDashboardServiceImpl implements ZineDashboardService {
    private final ZineMapper zineMapper;
    private final ZineReadingSessionMapper zineSessionMapper;
    private final ZineReadingProgressMapper zineProgressMapper;
    private final ZineReviewMapper zineReviewMapper;
    private final EpubAnnotationMapper epubAnnotationMapper;
    private final EpubBookmarkMapper epubBookmarkMapper;
    private final ReadingSessionMapper bookSessionMapper;
    private final ReadingProgressMapper bookProgressMapper;
    private final ChapterRatingMapper ratingMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";
    private static final String COMPLETED = "completed";
    private static final String READING = "reading";
    private static final String NOT_STARTED = "not_started";
    private static final String BOOKMARKED = "bookmarked";
    private static final String UP = "up";
    private static final String DOWN = "down";
    private static final String NEUTRAL = "neutral";

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
            return -1;
        }
    }

    private int resolveChapterFromProgress(ZineReadingProgress progress, ZineReadingSession session) {
        int cfiChapter = parseCfiChapter(progress != null ? progress.getCurrentPosition() : null);
        if (cfiChapter > 0) return cfiChapter;

        if (progress != null && progress.getCurrentPage() != null && progress.getCurrentPage() > 0) {
            return progress.getCurrentPage();
        }
        return session != null && session.getStartChapter() != null ? session.getStartChapter() : 0;
    }

    private User getCurrentUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.trim().isEmpty()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private String getAuthorName(Long zineId) {
        try {
            List<String> names = zineMapper.findAuthorNamesByZineId(zineId);
            return (names != null && !names.isEmpty()) ? names.get(0) : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public DataResponse<ZineLibraryPageResponse> getZineLibrary(String filter, int page, int limit, String sortBy) {
        User user = getCurrentUser();
        List<ZineReadingSession> allSessions = zineSessionMapper.findAllUserSessions(user.getId());

        Map<Long, ZineReadingSession> latestPerZine = new LinkedHashMap<>();
        for (ZineReadingSession session : allSessions) {
            latestPerZine.merge(session.getZineId(), session, (existing, incoming) -> {
                if (incoming.getStartedAt() != null && existing.getStartedAt() != null) {
                    return incoming.getStartedAt().isAfter(existing.getStartedAt()) ? incoming : existing;
                }
                return existing;
            });
        }

        List<ZineLibraryItemResponse> items = latestPerZine.entrySet().stream()
                .map(entry -> {
                    Zine zine = zineMapper.findById(entry.getKey());
                    if (zine == null) return null;

                    ZineReadingProgress progress = zineProgressMapper.findByUserAndZine(user.getId(), zine.getId());
                    double pct = progress != null && progress.getPercentageCompleted() != null
                            ? progress.getPercentageCompleted().doubleValue() : 0.0;

                    String status = pct >= 95.0 ? COMPLETED : pct > 0 ? READING : NOT_STARTED;

                    if (READING.equals(filter) && !READING.equals(status)) return null;
                    if (COMPLETED.equals(filter) && !COMPLETED.equals(status)) return null;
                    if (BOOKMARKED.equals(filter)) {
                        if (epubBookmarkMapper.countByUserAndBook(user.getId(), zine.getId()) == 0)
                            return null;
                    }

                    ZineReadingSession session = entry.getValue();
                    int displayChapter = resolveChapterFromProgress(progress, session);

                    ZineLibraryItemResponse r = new ZineLibraryItemResponse();
                    r.setZineId(zine.getId());
                    r.setZineSlug(zine.getSlug());
                    r.setZineTitle(zine.getTitle());
                    r.setAuthorName(getAuthorName(zine.getId()));
                    r.setCoverImageUrl(zine.getCoverImageUrl());
                    r.setProgressPercentage(pct);
                    r.setReadingStatus(status);
                    r.setLastReadAt(session.getStartedAt());
                    r.setBookmarkCount(epubBookmarkMapper.countByUserAndBook(user.getId(), zine.getId()));
                    r.setHighlightCount(epubAnnotationMapper.countByUserAndBook(user.getId(), zine.getId()));
                    r.setCurrentChapter(displayChapter);
                    r.setTotalChapters(progress != null && progress.getTotalPages() != null
                            ? progress.getTotalPages() : 0);
                    r.setLastCfi(progress != null ? progress.getCurrentPosition() : null);
                    return r;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Comparator<ZineLibraryItemResponse> comparator = switch (sortBy) {
            case "progress" ->
                    Comparator.comparing(ZineLibraryItemResponse::getProgressPercentage, Comparator.reverseOrder());
            case "title" ->
                    Comparator.comparing(ZineLibraryItemResponse::getZineTitle, Comparator.nullsLast(Comparator.naturalOrder()));
            default ->
                    Comparator.comparing(ZineLibraryItemResponse::getLastReadAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        items.sort(comparator);

        int total = items.size();
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);

        ZineLibraryPageResponse pageResponse = new ZineLibraryPageResponse();
        pageResponse.setItems(items.subList(fromIndex, toIndex));
        pageResponse.setTotalData(total);
        pageResponse.setPage(page);
        pageResponse.setLimit(limit);

        return new DataResponse<>(SUCCESS, "Library zine berhasil diambil",
                HttpStatus.OK.value(), pageResponse);
    }

    @Override
    public DataResponse<ZineHistoryPageResponse> getZineReadingHistory(int days, int page, int limit) {
        User user = getCurrentUser();

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<ZineReadingSession> sessions = zineSessionMapper.findUserSessionsSince(user.getId(), since);

        sessions.sort(Comparator.comparing(ZineReadingSession::getStartedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<ZineHistoryItemResponse> items = sessions.stream()
                .map(session -> {
                    Zine zine = zineMapper.findById(session.getZineId());
                    if (zine == null) return null;

                    ZineReadingProgress progress = zineProgressMapper.findByUserAndZine(user.getId(), zine.getId());
                    int lastKnownChapter = resolveChapterFromProgress(progress, session);

                    int durationSec = session.getTotalDurationSeconds() != null
                            ? session.getTotalDurationSeconds() : 0;
                    String durationStr = formatDuration(durationSec);

                    ZineHistoryItemResponse item = new ZineHistoryItemResponse();
                    item.setActivityId(session.getId());
                    item.setZineId(zine.getId());
                    item.setZineSlug(zine.getSlug());
                    item.setZineTitle(zine.getTitle());
                    item.setAuthorName(getAuthorName(zine.getId()));
                    item.setZineCover(zine.getCoverImageUrl());
                    item.setTimestamp(session.getStartedAt());
                    item.setChapterNumber(lastKnownChapter > 0 ? lastKnownChapter : null);
                    item.setDescription(String.format("Membaca Zine EPUB (bab %d) selama %s",
                            lastKnownChapter > 0 ? lastKnownChapter : 0, durationStr));
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

        ZineHistoryPageResponse pageResponse = new ZineHistoryPageResponse();
        pageResponse.setList(items.subList(fromIndex, toIndex));
        pageResponse.setTotal(total);
        pageResponse.setPage(page);
        pageResponse.setLimit(limit);

        return new DataResponse<>(SUCCESS, "Riwayat baca zine berhasil diambil",
                HttpStatus.OK.value(), pageResponse);
    }

    @Override
    public DataResponse<ZineStatisticsResponse> getZineStatistics(int period) {
        User user = getCurrentUser();
        LocalDateTime since = LocalDateTime.now().minusDays(period);

        List<ZineReadingSession> sessions = zineSessionMapper.findUserSessionsSince(user.getId(), since);

        ZineStatisticsResponse stats = new ZineStatisticsResponse();

        long totalZines = sessions.stream()
                .map(ZineReadingSession::getZineId).filter(Objects::nonNull).distinct().count();
        stats.setTotalZinesRead((int) totalZines);

        int totalSeconds = sessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0)
                .sum();
        stats.setTotalReadingMinutes(totalSeconds / 60);

        long totalChapters = sessions.stream()
                .mapToInt(s -> s.getChaptersRead() != null ? s.getChaptersRead() : 0)
                .sum();
        stats.setTotalChaptersRead((int) totalChapters);

        List<ZineReadingProgress> allProgress = zineProgressMapper.findAllByUser(user.getId());
        long completed = allProgress.stream()
                .filter(p -> p.getPercentageCompleted() != null && p.getPercentageCompleted().doubleValue() >= 95.0)
                .count();
        stats.setCompletedZines((int) completed);

        stats.setCompletionRate(allProgress.isEmpty() ? 0.0
                : allProgress.stream()
                .mapToDouble(p -> p.getPercentageCompleted() != null ? p.getPercentageCompleted().doubleValue() : 0.0)
                .average().orElse(0.0));

        int totalMinutes = totalSeconds / 60;
        if (totalMinutes > 0 && totalChapters > 0) {
            double wpm = Math.max(80, Math.min(600, (totalChapters * 3000.0) / totalMinutes));
            stats.setEstimatedReadingSpeedWpm(wpm);
        }

        LocalDateTime prevSince = since.minusDays(period);
        List<ZineReadingSession> prevSessions = zineSessionMapper.findUserSessionsBetween(user.getId(), prevSince, since);

        int prevMinutes = prevSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0)
                .sum();

        stats.setReadingTimeTrend(buildTrend(totalMinutes, prevMinutes, "waktu baca zine"));
        stats.setCompletionTrend(buildTrend(
                (int) totalZines,
                (int) prevSessions.stream().map(ZineReadingSession::getZineId).distinct().count(),
                "zine selesai"));

        stats.setGenreBreakdown(buildGenreBreakdown(sessions));
        stats.setPeakReadingTimes(buildPeakReadingTimes(sessions));

        return new DataResponse<>(SUCCESS, "Statistik zine berhasil diambil",
                HttpStatus.OK.value(), stats);
    }

    @Override
    public DatatableResponse<UserZineReviewItemResponse> getUserZineReviews(int page, int limit) {
        User user = getCurrentUser();
        int offset = (page - 1) * limit;

        List<ZineReview> reviews = zineReviewMapper.findByUser(user.getId(), offset, limit);
        int total = zineReviewMapper.countByUser(user.getId());

        List<UserZineReviewItemResponse> items = reviews.stream()
                .map(r -> {
                    Zine zine = zineMapper.findById(r.getZineId());
                    UserZineReviewItemResponse item = new UserZineReviewItemResponse();
                    item.setReviewId(r.getId());
                    item.setZineId(r.getZineId());
                    item.setZineTitle(zine != null ? zine.getTitle() : "");
                    item.setZineSlug(zine != null ? zine.getSlug() : "");
                    item.setZineCover(zine != null ? zine.getCoverImageUrl() : null);
                    item.setReviewContent(r.getContent());
                    item.setCreatedAt(r.getCreatedAt());
                    return item;
                })
                .toList();

        PageDataResponse<UserZineReviewItemResponse> pageData = new PageDataResponse<>(page, limit, total, items);
        return new DatatableResponse<>(SUCCESS, "Review zine berhasil diambil",
                HttpStatus.OK.value(), pageData);
    }

    @Override
    public DataResponse<CombinedOverviewStats> getCombinedOverviewStats() {
        User user = getCurrentUser();

        List<ReadingSession> bookSessions = bookSessionMapper.findAllUserSessions(user.getId());
        List<ReadingProgress> bookProgress = bookProgressMapper.findAllByUser(user.getId());

        int bookMinutes = bookSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0)
                .sum();
        long completedBooks = bookProgress.stream()
                .filter(p -> p.getPercentageCompleted() != null && p.getPercentageCompleted().doubleValue() >= 95.0)
                .count();

        List<ZineReadingSession> zineSessions = zineSessionMapper.findAllUserSessions(user.getId());
        List<ZineReadingProgress> zineProgress = zineProgressMapper.findAllByUser(user.getId());

        int zineMinutes = zineSessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0)
                .sum();
        long completedZines = zineProgress.stream()
                .filter(p -> p.getPercentageCompleted() != null && p.getPercentageCompleted().doubleValue() >= 95.0)
                .count();

        List<LocalDateTime> allDates = new ArrayList<>();
        bookSessions.stream().map(ReadingSession::getStartedAt).filter(Objects::nonNull).forEach(allDates::add);
        zineSessions.stream().map(ZineReadingSession::getStartedAt).filter(Objects::nonNull).forEach(allDates::add);

        List<Map<String, Object>> bookRatings = ratingMapper.findAllUserRatings(user.getId());
        double avgRating = bookRatings.isEmpty() ? 0.0
                : bookRatings.stream().mapToInt(r -> safeInt(r.get("rating"))).average().orElse(0.0);

        CombinedOverviewStats stats = new CombinedOverviewStats();
        stats.setTotalBooks((int) bookSessions.stream()
                .map(ReadingSession::getBookId).filter(Objects::nonNull).distinct().count());
        stats.setCompletedBooks((int) completedBooks);
        stats.setTotalBookReadingMinutes(bookMinutes);
        stats.setTotalZines((int) zineSessions.stream()
                .map(ZineReadingSession::getZineId).filter(Objects::nonNull).distinct().count());
        stats.setCompletedZines((int) completedZines);
        stats.setTotalZineReadingMinutes(zineMinutes);
        stats.setTotalReadingMinutes(bookMinutes + zineMinutes);
        stats.setCurrentStreak(calculateCurrentStreak(allDates));
        stats.setLongestStreak(calculateLongestStreak(allDates));
        stats.setAverageRating(avgRating);

        return new DataResponse<>(SUCCESS, "Overview gabungan berhasil diambil",
                HttpStatus.OK.value(), stats);
    }

    private String formatDuration(int totalSeconds) {
        if (totalSeconds >= 3600)
            return String.format("%d jam %d menit", totalSeconds / 3600, (totalSeconds % 3600) / 60);
        if (totalSeconds >= 60)
            return String.format("%d menit", totalSeconds / 60);
        return totalSeconds > 0 ? String.format("%d detik", totalSeconds) : "kurang dari 1 menit";
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

    private List<GenreBreakdownItem> buildGenreBreakdown(List<ZineReadingSession> sessions) {
        Map<String, List<ZineReadingSession>> byGenre = new HashMap<>();
        for (ZineReadingSession session : sessions) {
            Zine zine = zineMapper.findById(session.getZineId());
            if (zine == null || zine.getCategory() == null) continue;
            byGenre.computeIfAbsent(zine.getCategory(), k -> new ArrayList<>()).add(session);
        }
        int totalSessions = sessions.size();
        return byGenre.entrySet().stream()
                .map(entry -> {
                    int minutesSpent = entry.getValue().stream()
                            .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() / 60 : 0)
                            .sum();
                    GenreBreakdownItem item = new GenreBreakdownItem();
                    item.setGenreName(entry.getKey());
                    item.setBooksRead((int) entry.getValue().stream()
                            .map(ZineReadingSession::getZineId).distinct().count());
                    item.setMinutesSpent(minutesSpent);
                    item.setPercentage(totalSessions > 0 ? (entry.getValue().size() * 100.0) / totalSessions : 0);
                    item.setAverageRating(0.0);
                    return item;
                })
                .sorted(Comparator.comparing(GenreBreakdownItem::getMinutesSpent, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private List<PeakReadingTimeItem> buildPeakReadingTimes(List<ZineReadingSession> sessions) {
        Map<Integer, Integer> minutesByHour = new HashMap<>();
        for (ZineReadingSession session : sessions) {
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
            long daysBetween = Duration.between(dates.get(i - 1), dates.get(i)).toDays();
            if (daysBetween == 1) {
                maxStreak = Math.max(maxStreak, ++current);
            } else {
                current = 1;
            }
        }
        return maxStreak;
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