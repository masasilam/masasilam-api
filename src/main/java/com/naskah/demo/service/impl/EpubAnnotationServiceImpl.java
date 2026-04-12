package com.naskah.demo.service.impl;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.EpubAnnotationService;
import com.naskah.demo.util.interceptor.HeaderHolder;
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

    private final EpubAnnotationMapper  annotationMapper;
    private final EpubBookmarkMapper    bookmarkMapper;
    private final BookMapper            bookMapper;
    private final UserMapper            userMapper;
    private final ReadingSessionMapper  sessionMapper;
    private final ReadingProgressMapper readingProgressMapper;
    private final HeaderHolder          headerHolder;

    private static final String SESSION_TYPE_EPUB = "EPUB";
    private static final String SUCCESS           = "Success";

    private User getCurrentUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private Book getBook(String slug) {
        Book book = bookMapper.findBookBySlug(slug);
        if (book == null) throw new DataNotFoundException();
        return book;
    }

    private EpubAnnotationResponse toAnnotationResponse(EpubAnnotation a) {
        EpubAnnotationResponse r = new EpubAnnotationResponse();
        r.setId(a.getId()); r.setCfi(a.getCfi()); r.setSelectedText(a.getSelectedText());
        r.setColor(a.getColor()); r.setNote(a.getNote());
        r.setCreatedAt(a.getCreatedAt()); r.setUpdatedAt(a.getUpdatedAt());
        return r;
    }

    private EpubBookmarkResponse toBookmarkResponse(EpubBookmark b) {
        EpubBookmarkResponse r = new EpubBookmarkResponse();
        r.setId(b.getId()); r.setCfi(b.getCfi()); r.setLabel(b.getLabel()); r.setCreatedAt(b.getCreatedAt());
        return r;
    }

    @Override
    public DataResponse<EpubAnnotationsBundleResponse> getAll(String bookSlug) {
        User user = getCurrentUser();
        Book book = getBook(bookSlug);
        List<EpubAnnotationResponse> annotations = annotationMapper.findByUserAndBook(user.getId(), book.getId()).stream().map(this::toAnnotationResponse).toList();
        List<EpubBookmarkResponse> bookmarks = bookmarkMapper.findByUserAndBook(user.getId(), book.getId()).stream().map(this::toBookmarkResponse).toList();
        EpubAnnotationsBundleResponse bundle = new EpubAnnotationsBundleResponse();
        bundle.setAnnotations(annotations); bundle.setBookmarks(bookmarks);
        return new DataResponse<>(SUCCESS, "EPUB annotations retrieved", HttpStatus.OK.value(), bundle);
    }

    @Override
    @Transactional
    public DataResponse<EpubAnnotationResponse> addAnnotation(String bookSlug, EpubAnnotationRequest request) {
        User user = getCurrentUser();
        Book book = getBook(bookSlug);
        String color = (request.getColor() != null && !request.getColor().isBlank()) ? request.getColor() : "#FDE68A";
        EpubAnnotation annotation = new EpubAnnotation();
        annotation.setUserId(user.getId()); annotation.setBookId(book.getId());
        annotation.setCfi(request.getCfi()); annotation.setSelectedText(request.getSelectedText());
        annotation.setColor(color); annotation.setNote(request.getNote());
        annotation.setCreatedAt(LocalDateTime.now()); annotation.setUpdatedAt(LocalDateTime.now());
        annotationMapper.insert(annotation);
        log.info("EPUB annotation added: id={} user={} book={}", annotation.getId(), user.getId(), bookSlug);
        return new DataResponse<>(SUCCESS, "Annotation added", HttpStatus.CREATED.value(), toAnnotationResponse(annotation));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteAnnotation(String bookSlug, Long annotationId) {
        User user = getCurrentUser();
        EpubAnnotation annotation = annotationMapper.findById(annotationId);
        if (annotation == null) throw new DataNotFoundException();
        if (!annotation.getUserId().equals(user.getId())) throw new UnauthorizedException();
        annotationMapper.deleteById(annotationId);
        return new DataResponse<>(SUCCESS, "Annotation deleted", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<EpubBookmarkResponse> addBookmark(String bookSlug, EpubBookmarkRequest request) {
        User user = getCurrentUser();
        Book book = getBook(bookSlug);
        EpubBookmark bookmark = new EpubBookmark();
        bookmark.setUserId(user.getId()); bookmark.setBookId(book.getId());
        bookmark.setCfi(request.getCfi()); bookmark.setLabel(request.getLabel());
        bookmark.setCreatedAt(LocalDateTime.now());
        bookmarkMapper.insert(bookmark);
        return new DataResponse<>(SUCCESS, "Bookmark added", HttpStatus.CREATED.value(), toBookmarkResponse(bookmark));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteBookmark(String bookSlug, Long bookmarkId) {
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
        Book book = bookMapper.findBookBySlug(slug);
        if (book == null) throw new DataNotFoundException();

        EpubStartReadingResponse response = new EpubStartReadingResponse();

        int existingSessions = bookMapper.countUserReadSessions(book.getId(), user.getId());
        if (existingSessions == 0) {
            bookMapper.incrementReadCount(book.getId());
            response.setFirstTime(true);
            log.info("EPUB first-time read: user={} book={}", user.getId(), slug);
        } else {
            // Ambil posisi terakhir dari reading_progress
            ReadingProgress progress = readingProgressMapper.findByUserAndBook(user.getId(), book.getId());
            response.setFirstTime(false);

            if (progress != null) {
                response.setLastCfi(progress.getCurrentPosition());
                response.setLastProgress(progress.getPercentageCompleted() != null
                        ? progress.getPercentageCompleted().doubleValue() : null);
                response.setLastChapterIndex(progress.getCurrentPage() != null
                        ? progress.getCurrentPage() - 1 : null); // currentPage is 1-based
                response.setTotalChapters(progress.getTotalPages());
            }
        }

        return new DataResponse<>(SUCCESS, "EPUB reading started", HttpStatus.OK.value(), response);
    }

    @Override
    public DataResponse<Void> endReading(String slug, EndReadingRequest request) {
        log.debug("EPUB endReading (no-op): slug={}", slug);
        return new DataResponse<>(SUCCESS, "EPUB reading ended", HttpStatus.OK.value(), null);
    }

    /**
     * Merekam satu sesi baca EPUB.
     *
     * FIX:
     *   1. session.setSessionType("EPUB") — wajib agar DashboardService bisa bedakan
     *      EPUB vs Chapter via sessionType eksplisit, bukan startChapter==0 yang ambigu.
     *   2. Simpan lastCfi ke reading_progress.current_position agar dashboard
     *      bisa kirim CFI ini ke EpubReaderPage untuk resume ke posisi tepat.
     */
    @Override
    @Transactional
    public DataResponse<Void> recordEpubSession(String slug, EpubSessionRequest request) {
        User user = getCurrentUser();
        Book book = bookMapper.findBookBySlug(slug);
        if (book == null) throw new DataNotFoundException();

        if (sessionMapper.existsBySessionId(request.getSessionId())) {
            log.debug("EPUB session already recorded: sessionId={}", request.getSessionId());
            return new DataResponse<>(SUCCESS, "EPUB session already recorded", 200, null);
        }

        LocalDateTime endedAt         = LocalDateTime.now();
        int           durationSeconds = request.getDurationSeconds() != null ? request.getDurationSeconds() : 0;
        LocalDateTime startedAt       = endedAt.minusSeconds(durationSeconds);

        int spineIndexRaw   = request.getSpineIndex()     != null ? request.getSpineIndex()     : 0;
        int totalSpineItems = request.getTotalSpineItems() != null ? request.getTotalSpineItems() : 0;
        int chapterIndexFromToc = (request.getChapterIndex() != null) ? request.getChapterIndex() : spineIndexRaw;
        int chapterOneBased = chapterIndexFromToc + 1;
        String chapterLabel = (request.getChapterLabel() != null && !request.getChapterLabel().isBlank()) ? request.getChapterLabel() : "";

        int resolvedTotalPages;
        if (request.getTotalChapters() != null && request.getTotalChapters() > 0) {
            resolvedTotalPages = request.getTotalChapters();
        } else if (totalSpineItems > 0) {
            resolvedTotalPages = totalSpineItems;
        } else {
            resolvedTotalPages = 100;
        }

        double currentPct  = request.getProgressPercent() != null ? request.getProgressPercent().doubleValue() : 0.0;
        double previousPct = 0.0;

        ReadingProgress existingProgress = readingProgressMapper.findByUserAndBook(user.getId(), book.getId());
        if (existingProgress != null && existingProgress.getPercentageCompleted() != null) {
            previousPct = existingProgress.getPercentageCompleted().doubleValue();
        }
        double completionDelta = Math.max(0.0, currentPct - previousPct);

        // FIX: set sessionType = "EPUB" agar isEpubSession() di DashboardService benar
        ReadingSession session = new ReadingSession();
        session.setUserId(user.getId()); session.setBookId(book.getId());
        session.setSessionId(request.getSessionId()); session.setStartedAt(startedAt); session.setEndedAt(endedAt);
        session.setTotalDurationSeconds(durationSeconds);
        session.setSessionType(SESSION_TYPE_EPUB);
        session.setStartChapter(chapterOneBased); session.setEndChapter(chapterOneBased);
        session.setChaptersRead(1); session.setCompletionDelta(completionDelta);
        session.setTotalInteractions(0); session.setDeviceType(request.getDeviceType()); session.setDeviceId(null);
        session.setCreatedAt(LocalDateTime.now()); session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insertSession(session);

        if (currentPct > 0) {
            int    addedMins = durationSeconds / 60;
            String newStatus = currentPct >= 95.0 ? "completed" : "reading";

            // FIX: ambil CFI terakhir untuk disimpan ke current_position
            String lastCfi = (request.getLastCfi() != null && !request.getLastCfi().isBlank()) ? request.getLastCfi() : null;

            if (existingProgress == null) {
                ReadingProgress progress = new ReadingProgress();
                progress.setUserId(user.getId()); progress.setBookId(book.getId());
                progress.setPercentageCompleted(BigDecimal.valueOf(currentPct));
                progress.setReadingTimeMinutes(addedMins); progress.setStatus(newStatus);
                progress.setLastReadAt(endedAt); progress.setStartedAt(startedAt);
                progress.setCompletedAt("completed".equals(newStatus) ? endedAt : null);
                // Saat insert pertama kali, chapterOneBased=1 mungkin karena TOC belum load
                // Simpan 1 sebagai default (wajar untuk record pertama)
                progress.setCurrentPage(chapterOneBased);
                progress.setTotalPages(resolvedTotalPages);
                progress.setCurrentPosition(lastCfi);
                readingProgressMapper.insert(progress);
                log.info("EPUB progress created: user={} book={} pct={}% chapter={}/{} '{}' hasCfi={} status={} mins={}",
                        user.getId(), slug, currentPct, chapterOneBased, resolvedTotalPages, chapterLabel, lastCfi != null, newStatus, addedMins);
            } else {
                double existingPct = existingProgress.getPercentageCompleted() != null ? existingProgress.getPercentageCompleted().doubleValue() : 0.0;
                int prevMins = existingProgress.getReadingTimeMinutes() != null ? existingProgress.getReadingTimeMinutes() : 0;
                if (currentPct > existingPct) {
                    existingProgress.setPercentageCompleted(BigDecimal.valueOf(currentPct));
                    existingProgress.setStatus(newStatus); existingProgress.setLastReadAt(endedAt);
                    existingProgress.setReadingTimeMinutes(prevMins + addedMins);
                    // Hanya update currentPage kalau chapterIndex benar-benar dikirim (> 0)
                    // chapterIndexFromToc == 0 bisa berarti belum load TOC, bukan bab pertama
                    if (chapterIndexFromToc > 0) {
                        existingProgress.setCurrentPage(chapterOneBased);
                    }
                    existingProgress.setTotalPages(resolvedTotalPages);
                    existingProgress.setCurrentPosition(lastCfi);
                    if ("completed".equals(newStatus) && existingProgress.getCompletedAt() == null) {
                        existingProgress.setCompletedAt(endedAt);
                    }
                    readingProgressMapper.update(existingProgress);
                    log.info("EPUB progress updated: user={} book={} {}%->{}% chapter={}/{} '{}' hasCfi={} status={} totalMins={}",
                            user.getId(), slug, existingPct, currentPct, chapterOneBased, resolvedTotalPages, chapterLabel,
                            lastCfi != null, newStatus, existingProgress.getReadingTimeMinutes());
                } else {
                    // Progress tidak maju — hanya tambah waktu & update CFI
                    existingProgress.setReadingTimeMinutes(prevMins + addedMins);
                    existingProgress.setLastReadAt(endedAt);
                    if (lastCfi != null) existingProgress.setCurrentPosition(lastCfi);
                    readingProgressMapper.updateTimeOnly(existingProgress);
                }
            }
        }

        log.info("EPUB session recorded: user={} book={} duration={}s progress={}% chapter={}/{} '{}' delta={}%",
                user.getId(), slug, durationSeconds, currentPct, chapterOneBased, resolvedTotalPages, chapterLabel, completionDelta);
        return new DataResponse<>(SUCCESS, "EPUB session recorded", 200, null);
    }
}