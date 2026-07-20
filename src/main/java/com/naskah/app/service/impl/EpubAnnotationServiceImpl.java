package com.naskah.app.service.impl;

import com.naskah.app.exception.custom.DataNotFoundException;
import com.naskah.app.exception.custom.UnauthorizedException;
import com.naskah.app.mapper.*;
import com.naskah.app.model.dto.request.*;
import com.naskah.app.model.dto.response.*;
import com.naskah.app.model.entity.*;
import com.naskah.app.service.EpubAnnotationService;
import com.naskah.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpubAnnotationServiceImpl implements EpubAnnotationService {
    private final EpubAnnotationMapper annotationMapper;
    private final EpubBookmarkMapper bookmarkMapper;
    private final BookMapper bookMapper;
    private final ZineMapper zineMapper;
    private final UserMapper userMapper;
    private final ReadingSessionMapper sessionMapper;
    private final ReadingProgressMapper readingProgressMapper;
    private final HeaderHolder headerHolder;

    private static final String SESSION_TYPE_EPUB = "EPUB";
    private static final String SUCCESS = "Success";
    private static final String CCOMPLETED = "completed";

    private User getCurrentUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private long[] resolveContentId(String slug) {
        Book book = bookMapper.findBookBySlug(slug);
        if (book != null) return new long[]{book.getId(), 0};

        Zine zine = zineMapper.findZineBySlug(slug);
        if (zine != null) return new long[]{0, zine.getId()};

        throw new DataNotFoundException();
    }

    private boolean isBook(long[] ids) {
        return ids[0] > 0;
    }

    private EpubAnnotationResponse toAnnotationResponse(EpubAnnotation a) {
        EpubAnnotationResponse r = new EpubAnnotationResponse();
        r.setId(a.getId());
        r.setCfi(a.getCfi());
        r.setSelectedText(a.getSelectedText());
        r.setColor(a.getColor());
        r.setNote(a.getNote());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        return r;
    }

    private EpubBookmarkResponse toBookmarkResponse(EpubBookmark b) {
        EpubBookmarkResponse r = new EpubBookmarkResponse();
        r.setId(b.getId());
        r.setCfi(b.getCfi());
        r.setLabel(b.getLabel());
        r.setCreatedAt(b.getCreatedAt());
        return r;
    }

    @Override
    public DataResponse<EpubAnnotationsBundleResponse> getAll(String slug) {
        User user = getCurrentUser();
        long[] ids = resolveContentId(slug);

        List<EpubAnnotationResponse> annotations = isBook(ids)
                ? annotationMapper.findByUserAndBook(user.getId(), ids[0]).stream().map(this::toAnnotationResponse).toList()
                : annotationMapper.findByUserAndZine(user.getId(), ids[1]).stream().map(this::toAnnotationResponse).toList();

        List<EpubBookmarkResponse> bookmarks = isBook(ids)
                ? bookmarkMapper.findByUserAndBook(user.getId(), ids[0]).stream().map(this::toBookmarkResponse).toList()
                : bookmarkMapper.findByUserAndZine(user.getId(), ids[1]).stream().map(this::toBookmarkResponse).toList();

        EpubAnnotationsBundleResponse bundle = new EpubAnnotationsBundleResponse();
        bundle.setAnnotations(annotations);
        bundle.setBookmarks(bookmarks);
        return new DataResponse<>(SUCCESS, "EPUB annotations retrieved", HttpStatus.OK.value(), bundle);
    }

    @Override
    @Transactional
    public DataResponse<EpubAnnotationResponse> addAnnotation(String slug, EpubAnnotationRequest request) {
        User user = getCurrentUser();
        long[] ids = resolveContentId(slug);

        String color = (request.getColor() != null && !request.getColor().isBlank()) ? request.getColor() : "#FDE68A";

        EpubAnnotation annotation = new EpubAnnotation();
        annotation.setUserId(user.getId());
        annotation.setBookId(isBook(ids) ? ids[0] : null);
        annotation.setZineId(!isBook(ids) ? ids[1] : null);
        annotation.setCfi(request.getCfi());
        annotation.setSelectedText(request.getSelectedText());
        annotation.setColor(color);
        annotation.setNote(request.getNote());
        annotation.setCreatedAt(LocalDateTime.now());
        annotation.setUpdatedAt(LocalDateTime.now());
        annotationMapper.insert(annotation);

        log.info("EPUB annotation added: id={} user={} slug={} type={}",
                annotation.getId(), user.getId(), slug, isBook(ids) ? "book" : "zine");
        return new DataResponse<>(SUCCESS, "Annotation added", HttpStatus.CREATED.value(), toAnnotationResponse(annotation));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteAnnotation(String slug, Long annotationId) {
        User user = getCurrentUser();
        EpubAnnotation annotation = annotationMapper.findById(annotationId);
        if (annotation == null) throw new DataNotFoundException();
        if (!annotation.getUserId().equals(user.getId())) throw new UnauthorizedException();
        annotationMapper.deleteById(annotationId);
        return new DataResponse<>(SUCCESS, "Annotation deleted", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<EpubBookmarkResponse> addBookmark(String slug, EpubBookmarkRequest request) {
        User user = getCurrentUser();
        long[] ids = resolveContentId(slug);

        EpubBookmark bookmark = new EpubBookmark();
        bookmark.setUserId(user.getId());
        bookmark.setBookId(isBook(ids) ? ids[0] : null);
        bookmark.setZineId(!isBook(ids) ? ids[1] : null);
        bookmark.setCfi(request.getCfi());
        bookmark.setLabel(request.getLabel());
        bookmark.setCreatedAt(LocalDateTime.now());
        bookmarkMapper.insert(bookmark);

        return new DataResponse<>(SUCCESS, "Bookmark added", HttpStatus.CREATED.value(), toBookmarkResponse(bookmark));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteBookmark(String slug, Long bookmarkId) {
        User user = getCurrentUser();
        EpubBookmark bookmark = bookmarkMapper.findById(bookmarkId);
        if (bookmark == null) throw new DataNotFoundException();
        if (!bookmark.getUserId().equals(user.getId())) throw new UnauthorizedException();
        bookmarkMapper.deleteById(bookmarkId);
        return new DataResponse<>(SUCCESS, "Bookmark deleted", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<EpubStartReadingResponse> startReading(String slug, EpubStartReadingRequest request) {
        User user = getCurrentUser();
        long[] ids = resolveContentId(slug);

        EpubStartReadingResponse response = new EpubStartReadingResponse();

        if (isBook(ids)) {
            long bookId = ids[0];
            int existingSessions = bookMapper.countUserReadSessions(bookId, user.getId());
            if (existingSessions == 0) {
                bookMapper.incrementReadCount(bookId);
                response.setFirstTime(true);
                log.info("EPUB first-time read: user={} book={}", user.getId(), slug);
            } else {
                ReadingProgress progress = readingProgressMapper.findByUserAndBook(user.getId(), bookId);
                response.setFirstTime(false);
                if (progress != null) {
                    response.setLastCfi(progress.getCurrentPosition());
                    response.setLastProgress(progress.getPercentageCompleted() != null ? progress.getPercentageCompleted().doubleValue() : null);
                    response.setLastChapterIndex(progress.getCurrentPage() != null ? progress.getCurrentPage() - 1 : null);
                    response.setTotalChapters(progress.getTotalPages());
                    response.setLastReadAt(progress.getLastReadAt());
                }
            }
        } else {
            long zineId = ids[1];
            int existingSessions = zineMapper.countUserReadSessions(zineId, user.getId());
            if (existingSessions == 0) {
                zineMapper.incrementReadCount(zineId);
                response.setFirstTime(true);
                log.info("EPUB first-time read: user={} zine={}", user.getId(), slug);
            } else {
                ReadingProgress progress = readingProgressMapper.findByUserAndZine(user.getId(), zineId);
                response.setFirstTime(false);
                if (progress != null) {
                    response.setLastCfi(progress.getCurrentPosition());
                    response.setLastProgress(progress.getPercentageCompleted() != null ? progress.getPercentageCompleted().doubleValue() : null);
                    response.setLastChapterIndex(progress.getCurrentPage() != null ? progress.getCurrentPage() - 1 : null);
                    response.setTotalChapters(progress.getTotalPages());
                    response.setLastReadAt(progress.getLastReadAt());
                }
            }
        }

        return new DataResponse<>(SUCCESS, "EPUB reading started", HttpStatus.OK.value(), response);
    }

    @Override
    public DataResponse<Void> endReading(String slug, EndReadingRequest request) {
        log.debug("EPUB endReading (no-op): slug={}", slug);
        return new DataResponse<>(SUCCESS, "EPUB reading ended", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> recordEpubSession(String slug, EpubSessionRequest request) {
        User user = getCurrentUser();
        long[] ids = resolveContentId(slug);

        if (sessionMapper.existsBySessionId(request.getSessionId())) {
            log.debug("EPUB session already recorded: sessionId={}", request.getSessionId());
            return new DataResponse<>(SUCCESS, "EPUB session already recorded", 200, null);
        }

        LocalDateTime endedAt = LocalDateTime.now();
        int durationSeconds = request.getDurationSeconds() != null ? request.getDurationSeconds() : 0;
        LocalDateTime startedAt = endedAt.minusSeconds(durationSeconds);

        int spineIndexRaw = request.getSpineIndex() != null ? request.getSpineIndex() : 0;
        int totalSpineItems = request.getTotalSpineItems() != null ? request.getTotalSpineItems() : 0;
        int chapterIndexFromToc = request.getChapterIndex() != null ? request.getChapterIndex() : spineIndexRaw;
        int chapterOneBased = chapterIndexFromToc + 1;
        int resolvedTotalPages;
        if (request.getTotalChapters() != null && request.getTotalChapters() > 0) {
            resolvedTotalPages = request.getTotalChapters();
        } else if (totalSpineItems > 0) {
            resolvedTotalPages = totalSpineItems;
        } else {
            resolvedTotalPages = 100;
        }

        double currentPct = request.getProgressPercent() != null ? request.getProgressPercent().doubleValue() : 0.0;
        boolean progressIsAccurate = request.getProgressIsAccurate() == null || request.getProgressIsAccurate();
        String lastCfi = (request.getLastCfi() != null && !request.getLastCfi().isBlank()) ? request.getLastCfi() : null;

        ReadingProgress existingProgress = isBook(ids) ? readingProgressMapper.findByUserAndBook(user.getId(), ids[0]) : readingProgressMapper.findByUserAndZine(user.getId(), ids[1]);

        double previousPct = existingProgress != null && existingProgress.getPercentageCompleted() != null ? existingProgress.getPercentageCompleted().doubleValue() : 0.0;
        double completionDelta = Math.max(0.0, currentPct - previousPct);

        ReadingSession session = new ReadingSession();
        session.setUserId(user.getId());
        session.setBookId(isBook(ids) ? ids[0] : null);
        session.setZineId(!isBook(ids) ? ids[1] : null);
        session.setSessionId(request.getSessionId());
        session.setStartedAt(startedAt);
        session.setEndedAt(endedAt);
        session.setTotalDurationSeconds(durationSeconds);
        session.setSessionType(SESSION_TYPE_EPUB);
        session.setStartChapter(chapterOneBased);
        session.setEndChapter(chapterOneBased);
        session.setChaptersRead(1);
        session.setCompletionDelta(completionDelta);
        session.setTotalInteractions(0);
        session.setDeviceType(request.getDeviceType());
        session.setDeviceId(null);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insertSession(session);

        int addedMins = durationSeconds / 60;

        if (existingProgress == null) {
            String newStatus;
            if (currentPct >= 95.0) {
                newStatus = CCOMPLETED;
            } else if (currentPct > 0) {
                newStatus = "reading";
            } else {
                newStatus = "started";
            }

            ReadingProgress progress = new ReadingProgress();
            progress.setUserId(user.getId());
            progress.setBookId(isBook(ids) ? ids[0] : null);
            progress.setZineId(!isBook(ids) ? ids[1] : null);
            progress.setPercentageCompleted(BigDecimal.valueOf(currentPct));
            progress.setReadingTimeMinutes(addedMins);
            progress.setStatus(newStatus);
            progress.setLastReadAt(endedAt);
            progress.setStartedAt(startedAt);
            progress.setCompletedAt(CCOMPLETED.equals(newStatus) ? endedAt : null);
            progress.setCurrentPage(chapterOneBased);
            progress.setTotalPages(resolvedTotalPages);
            progress.setCurrentPosition(lastCfi);
            readingProgressMapper.insert(progress);

            log.info("EPUB progress created: user={} slug={} type={} pct={}% status={}", user.getId(), slug, isBook(ids) ? "book" : "zine", currentPct, newStatus);

        } else {
            double existingPct = existingProgress.getPercentageCompleted() != null ? existingProgress.getPercentageCompleted().doubleValue() : 0.0;
            int prevMins = existingProgress.getReadingTimeMinutes() != null ? existingProgress.getReadingTimeMinutes() : 0;

            boolean shouldUpdateProgress = progressIsAccurate ? currentPct > existingPct : currentPct > existingPct && existingPct == 0.0;

            if (shouldUpdateProgress) {
                String newStatus;
                if (currentPct >= 95.0) {
                    newStatus = CCOMPLETED;
                } else if (currentPct > 0) {
                    newStatus = "reading";
                } else {
                    newStatus = "started";
                }

                existingProgress.setPercentageCompleted(BigDecimal.valueOf(currentPct));
                existingProgress.setStatus(newStatus);
                existingProgress.setLastReadAt(endedAt);
                existingProgress.setReadingTimeMinutes(prevMins + addedMins);
                if (chapterIndexFromToc > 0) existingProgress.setCurrentPage(chapterOneBased);
                existingProgress.setTotalPages(resolvedTotalPages);
                existingProgress.setCurrentPosition(lastCfi);
                if (CCOMPLETED.equals(newStatus) && existingProgress.getCompletedAt() == null) {
                    existingProgress.setCompletedAt(endedAt);
                }
                readingProgressMapper.update(existingProgress);

                log.info("EPUB progress updated: user={} slug={} {}%->{}% status={}", user.getId(), slug, existingPct, currentPct, newStatus);
            } else {
                existingProgress.setReadingTimeMinutes(prevMins + addedMins);
                existingProgress.setLastReadAt(endedAt);
                if (lastCfi != null) existingProgress.setCurrentPosition(lastCfi);
                readingProgressMapper.updateTimeOnly(existingProgress);

                log.debug("EPUB time-only update: user={} slug={} addedMins={}", user.getId(), slug, addedMins);
            }
        }

        log.info("EPUB session recorded: user={} slug={} type={} duration={}s progress={}%", user.getId(), slug, isBook(ids) ? "book" : "zine", durationSeconds, currentPct);

        return new DataResponse<>(SUCCESS, "EPUB session recorded", 200, null);
    }
}