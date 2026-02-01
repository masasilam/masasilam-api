package com.naskah.demo.service.book.impl;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.book.BookChapterService;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookChapterServiceImpl implements BookChapterService {
    private final BookMapper bookMapper;
    private final BookChapterMapper chapterMapper;
    private final UserMapper userMapper;
    private final BookmarkMapper bookmarkMapper;
    private final HighlightMapper highlightMapper;
    private final NoteMapper noteMapper;
    private final ChapterReviewMapper chapterReviewMapper;
    private final ChapterProgressMapper chapterProgressMapper;
    private final AnalyticsMapper analyticsMapper;
    private final ReadingProgressMapper readingProgressMapper;
    private final ChapterRatingMapper ratingMapper;
    private final ReadingActivityMapper activityMapper;
    private final ReadingSessionMapper sessionMapper;
    private final UserReadingPatternMapper patternMapper;
    private final SearchMapper searchMapper;
    private final AnnotationExportMapper exportMapper;
    private final EntityResponseMapper entityMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";
    private static final String TOTAL_RATINGS = "total_ratings";
    private static final String COUNT = "count";
    private static final String CHAPTER_NUMBER = "chapter_number";
    private static final String CHAPTER_TITLE = "chapter_title";
    private static final String UNKNOWN = "Unknown";
    private static final String MODERATE = "Moderate";
    private static final String AVG_READING_TIME = "avg_reading_time";
    private static final String HIGHLIGHT_COUNT = "highlight_count";

    // ============================================
    // CHAPTER READING
    // ============================================

    @Override
    @Cacheable(value = "chapter-by-path", key = "#slug + ':' + #slugPath")
    public DataResponse<ChapterReadingResponse> readChapterBySlugPath(String bookSlug, String slugPath) {
        try {
            // Validate and get book
            Book book = bookMapper.findBookBySlug(bookSlug);
            validateBook(book);

            // Parse slug hierarchy
            String[] slugParts = slugPath.split("/");
            BookChapter chapter = findChapterBySlugHierarchy(book.getId(), slugParts);

            if (chapter == null) {
                throw new DataNotFoundException();
            }

            // Build complete response
            ChapterReadingResponse response = buildChapterReadingResponse(book, chapter);

            return new DataResponse<>(SUCCESS, "Chapter retrieved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error reading chapter by path {}: {}", slugPath, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "chapter-list", key = "#slug")
    public DataResponse<List<ChapterSummaryResponse>> getAllChaptersSummary(String slug) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            List<BookChapter> chapters = chapterMapper.findChaptersByBookId(book.getId());

            // Get user ID if authenticated
            Long userId = getCurrentUserIdOrNull();

            // Build hierarchical structure
            List<ChapterSummaryResponse> hierarchicalChapters = buildChapterHierarchy(chapters, book.getId(), userId);

            return new DataResponse<>(SUCCESS, "Chapters retrieved successfully", HttpStatus.OK.value(), hierarchicalChapters);

        } catch (Exception e) {
            log.error("Error getting chapters for book {}: {}", slug, e.getMessage(), e);
            throw e;
        }
    }

    // ============================================
    // CHAPTER PROGRESS
    // ============================================

    @Override
    @Transactional
    public DataResponse<ChapterProgressResponse> saveChapterProgress(String slug, Integer chapterNumber, ChapterProgressRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            ChapterProgress progress = chapterProgressMapper.findProgress(user.getId(), book.getId(), chapterNumber);

            if (progress == null) {
                progress = new ChapterProgress();
                progress.setUserId(user.getId());
                progress.setBookId(book.getId());
                progress.setChapterNumber(chapterNumber);
                progress.setPosition(request.getPosition());
                progress.setReadingTimeSeconds(request.getReadingTimeSeconds());
                progress.setIsCompleted(request.getIsCompleted());
                progress.setLastReadAt(LocalDateTime.now());
                progress.setCreatedAt(LocalDateTime.now());

                chapterProgressMapper.insertProgress(progress);
            } else {
                progress.setPosition(request.getPosition());
                progress.setReadingTimeSeconds(progress.getReadingTimeSeconds() + request.getReadingTimeSeconds());
                progress.setIsCompleted(request.getIsCompleted());
                progress.setLastReadAt(LocalDateTime.now());

                chapterProgressMapper.updateProgress(progress);
            }

            // Update overall book progress for Dashboard
            updateOverallBookProgress(user.getId(), book.getId());

            ChapterProgressResponse response = new ChapterProgressResponse();
            response.setChapterNumber(chapterNumber);
            response.setPosition(progress.getPosition());
            response.setReadingTimeSeconds(progress.getReadingTimeSeconds());
            response.setIsCompleted(progress.getIsCompleted());
            response.setLastReadAt(progress.getLastReadAt());

            log.info("Progress saved for user {} on chapter {} of book {}", user.getId(), chapterNumber, slug);

            return new DataResponse<>(SUCCESS, "Progress saved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error saving chapter progress: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void updateOverallBookProgress(Long userId, Long bookId) {
        try {
            List<ChapterProgress> allProgress = chapterProgressMapper.findAllByUserAndBook(userId, bookId);
            long completedCount = allProgress.stream().filter(ChapterProgress::getIsCompleted).count();

            Book book = bookMapper.findById(bookId);
            if (book != null) {
                double percentage = (double) completedCount / book.getTotalPages() * 100;

                ReadingProgress overallProgress = readingProgressMapper.findByUserAndBook(userId, bookId);
                if (overallProgress != null) {
                    overallProgress.setPercentageCompleted(BigDecimal.valueOf(percentage));
                    overallProgress.setLastReadAt(LocalDateTime.now());
                    readingProgressMapper.updateReadingProgress(overallProgress);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update overall book progress: {}", e.getMessage());
        }
    }

    // ============================================
    // CHAPTER ANNOTATIONS (ADD)
    // ============================================

    @Override
    @Transactional
    public DataResponse<BookmarkResponse> addChapterBookmark(String slug, Integer chapterNumber, ChapterBookmarkRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            Bookmark bookmark = new Bookmark();
            bookmark.setUserId(user.getId());
            bookmark.setBookId(book.getId());
            bookmark.setChapterNumber(chapterNumber);
            bookmark.setChapterTitle(chapterMapper.getChapterTitle(book.getId(), chapterNumber));
            bookmark.setChapterSlug(chapterMapper.getChapterSlug(book.getId(), chapterNumber));
            bookmark.setPosition(request.getPosition());
            bookmark.setCreatedAt(LocalDateTime.now());

            bookmarkMapper.insertBookmark(bookmark);

            BookmarkResponse response = entityMapper.toBookmarkResponse(bookmark);

            return new DataResponse<>(SUCCESS, "Bookmark added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding chapter bookmark: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<HighlightResponse> addChapterHighlight(String slug, Integer chapterNumber, ChapterHighlightRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            Highlight highlight = new Highlight();
            highlight.setUserId(user.getId());
            highlight.setBookId(book.getId());
            highlight.setChapterNumber(chapterNumber);
            highlight.setChapterTitle(chapterMapper.getChapterTitle(book.getId(), chapterNumber));
            highlight.setChapterSlug(chapterMapper.getChapterSlug(book.getId(), chapterNumber));
            highlight.setStartPosition(request.getStartPosition());
            highlight.setEndPosition(request.getEndPosition());
            highlight.setHighlightedText(request.getHighlightedText());
            highlight.setColor(request.getColor());
            highlight.setCreatedAt(LocalDateTime.now());
            highlight.setUpdatedAt(LocalDateTime.now());

            highlightMapper.insertHighlight(highlight);
            analyticsMapper.updateHighlightHeatmap(book.getId(), chapterNumber, 1);

            HighlightResponse response = entityMapper.toHighlightResponse(highlight);

            log.info("Highlight added by user {} to chapter {} of book {}", user.getId(), chapterNumber, slug);

            return new DataResponse<>(SUCCESS, "Highlight added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding chapter highlight: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<NoteResponse> addChapterNote(String slug, Integer chapterNumber, ChapterNoteRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            Note note = new Note();
            note.setUserId(user.getId());
            note.setBookId(book.getId());
            note.setChapterNumber(chapterNumber);
            note.setChapterTitle(chapterMapper.getChapterTitle(book.getId(), chapterNumber));
            note.setChapterSlug(chapterMapper.getChapterSlug(book.getId(), chapterNumber));
            note.setPosition(request.getPosition());
            note.setContent(request.getContent());
            note.setSelectedText(request.getSelectedText());
            note.setCreatedAt(LocalDateTime.now());
            note.setUpdatedAt(LocalDateTime.now());

            noteMapper.insertNote(note);
            analyticsMapper.updateNoteHeatmap(book.getId(), chapterNumber, 1);

            NoteResponse response = entityMapper.toNoteResponse(note);

            log.info("Note added by user {} to chapter {} of book {}", user.getId(), chapterNumber, slug);

            return new DataResponse<>(SUCCESS, "Note added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding chapter note: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============================================
    // CHAPTER ANNOTATIONS (DELETE)
    // ============================================

    @Override
    @Transactional
    public DataResponse<Void> deleteChapterBookmark(String slug, Integer chapterNumber, Long bookmarkId) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            Bookmark bookmark = bookmarkMapper.findBookmarkById(bookmarkId);
            if (bookmark == null) {
                throw new DataNotFoundException();
            }

            if (!bookmark.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            bookmarkMapper.deleteBookmark(bookmarkId);

            log.info("Bookmark {} deleted by user {} from book {}", bookmarkId, user.getId(), slug);

            return new DataResponse<>(SUCCESS, "Bookmark deleted successfully", HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting bookmark: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteChapterHighlight(String slug, Integer chapterNumber, Long highlightId) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            Highlight highlight = highlightMapper.findHighlightById(highlightId);
            if (highlight == null) {
                throw new DataNotFoundException();
            }

            if (!highlight.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            highlightMapper.deleteHighlight(highlightId);
            analyticsMapper.updateHighlightHeatmap(book.getId(), chapterNumber, -1);

            log.info("Highlight {} deleted by user {} from book {}", highlightId, user.getId(), slug);

            return new DataResponse<>(SUCCESS, "Highlight deleted successfully", HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting highlight: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteChapterNote(String slug, Integer chapterNumber, Long noteId) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            Note note = noteMapper.findNoteById(noteId);
            if (note == null) {
                throw new DataNotFoundException();
            }

            if (!note.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            noteMapper.deleteNote(noteId);
            analyticsMapper.updateNoteHeatmap(book.getId(), chapterNumber, -1);

            log.info("Note {} deleted by user {} from book {}", noteId, user.getId(), slug);

            return new DataResponse<>(SUCCESS, "Note deleted successfully", HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting note: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============================================
    // HELPER METHODS - Building Responses
    // ============================================

    private ChapterReadingResponse buildChapterReadingResponse(Book book, BookChapter chapter) {
        ChapterReadingResponse response = new ChapterReadingResponse();

        response.setBookId(book.getId());
        response.setBookTitle(book.getTitle());
        response.setBookSubtitle(book.getSubtitle());
        response.setChapterId(chapter.getId());
        response.setChapterNumber(chapter.getChapterNumber());
        response.setChapterTitle(chapter.getTitle());
        response.setSlug(chapter.getSlug());
        response.setContent(chapter.getContent());
        response.setHtmlContent(chapter.getHtmlContent());
        response.setWordCount(chapter.getWordCount());
        response.setEstimatedReadTime(calculateReadTime(chapter.getWordCount()));
        response.setTotalChapters(book.getTotalPages());

        response.setParentChapterId(chapter.getParentChapterId());
        response.setChapterLevel(chapter.getChapterLevel());

        response.setBreadcrumbs(buildBreadcrumbs(chapter));
        setChapterNavigation(response, book.getId(), chapter);

        // Get user-specific data if authenticated
        Long userId = getCurrentUserIdOrNull();
        if (userId != null) {
            response.setBookmarks(getUserChapterBookmarks(userId, book.getId(), chapter.getChapterNumber()));
            response.setHighlights(getUserChapterHighlights(userId, book.getId(), chapter.getChapterNumber()));
            response.setNotes(getUserChapterNotes(userId, book.getId(), chapter.getChapterNumber()));

            ChapterProgress progress = chapterProgressMapper.findProgress(userId, book.getId(), chapter.getChapterNumber());
            if (progress != null) {
                response.setCurrentPosition(progress.getPosition());
                response.setIsCompleted(progress.getIsCompleted());
            }

            updateReadingHeatmap(book.getId(), chapter.getChapterNumber());
        }

        return response;
    }

    private void updateReadingHeatmap(Long bookId, Integer chapterNumber) {
        try {
            analyticsMapper.updateReadingHeatmap(bookId, chapterNumber, 1);
        } catch (Exception e) {
            log.warn("Failed to update reading heatmap: {}", e.getMessage());
        }
    }

    private int calculateReadTime(int wordCount) {
        return Math.max(1, wordCount / 200);
    }

    // ============================================
    // CHAPTER REVIEWS & SOCIAL
    // ============================================

    @Override
    public DataResponse<List<ChapterReviewResponse>> getChapterReviews(String slug, Integer chapterNumber, int page, int limit) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            int offset = (page - 1) * limit;
            List<ChapterReview> reviews = chapterReviewMapper.findReviewsByChapter(book.getId(), chapterNumber, offset, limit);

            Long currentUserId = getCurrentUserIdOrNull();

            List<ChapterReviewResponse> responses = reviews.stream()
                    .map(review -> mapToChapterReviewResponse(review, currentUserId))
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Reviews retrieved successfully", HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error getting chapter reviews: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<ChapterReviewResponse> addChapterReview(String slug, Integer chapterNumber, ChapterReviewRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            ChapterReview review = new ChapterReview();
            review.setUserId(user.getId());
            review.setBookId(book.getId());
            review.setChapterNumber(chapterNumber);
            review.setContent(request.getComment());
            review.setIsSpoiler(request.getIsSpoiler());
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());

            chapterReviewMapper.insertChapterReview(review);

            ChapterReviewResponse response = mapToChapterReviewResponse(review, user.getId());

            log.info("Review added by user {} to chapter {} of book {}", user.getId(), chapterNumber, slug);

            return new DataResponse<>(SUCCESS, "Review added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding chapter review: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<ChapterReviewResponse> replyToChapterReview(
            String slug, Integer chapterNumber, Long reviewId, ChapterReplyRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            ChapterReview parentReview = chapterReviewMapper.findById(reviewId);
            if (parentReview == null) {
                throw new DataNotFoundException();
            }

            ChapterReview reply = new ChapterReview();
            reply.setUserId(user.getId());
            reply.setBookId(book.getId());
            reply.setChapterNumber(chapterNumber);
            reply.setContent(request.getComment());
            reply.setParentId(reviewId);
            reply.setCreatedAt(LocalDateTime.now());
            reply.setUpdatedAt(LocalDateTime.now());

            chapterReviewMapper.insertChapterReview(reply);

            ChapterReviewResponse response = mapToChapterReviewResponse(reply, user.getId());

            log.info("Reply added by user {} to review {} of book {}", user.getId(), reviewId, slug);

            return new DataResponse<>(SUCCESS, "Reply added successfully", HttpStatus.CREATED.value(), response);
        } catch (Exception e) {
            log.error("Error adding chapter review reply: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> likeChapterReview(String slug, Integer chapterNumber, Long reviewId) {
        try {
            User user = getCurrentUser();

            chapterReviewMapper.likeReview(reviewId, user.getId());
            chapterReviewMapper.incrementLikeCount(reviewId);

            return new DataResponse<>(SUCCESS, "Review liked successfully", HttpStatus.OK.value(), null);
        } catch (Exception e) {
            log.error("Error liking chapter review: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> unlikeChapterReview(
            String slug, Integer chapterNumber, Long reviewId) {
        try {
            User user = getCurrentUser();

            chapterReviewMapper.unlikeReview(reviewId, user.getId());
            chapterReviewMapper.decrementLikeCount(reviewId);

            return new DataResponse<>(SUCCESS, "Review unliked successfully", HttpStatus.OK.value(), null);
        } catch (Exception e) {
            log.error("Error unliking chapter review: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============================================
    // CHAPTER RATING
    // ============================================

    @Override
    @Transactional
    public DataResponse<ChapterRatingResponse> rateChapter(String slug, Integer chapterNumber, ChapterRatingRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);
            if (chapter == null) {
                throw new DataNotFoundException();
            }

            ChapterRating rating = new ChapterRating();
            rating.setUserId(user.getId());
            rating.setBookId(book.getId());
            rating.setChapterNumber(chapterNumber);
            rating.setRating(request.getRating());
            rating.setCreatedAt(LocalDateTime.now());
            rating.setUpdatedAt(LocalDateTime.now());

            ratingMapper.upsertRating(rating);

            ChapterRatingResponse response = new ChapterRatingResponse();
            response.setId(rating.getId());
            response.setUserId(user.getId());
            response.setChapterNumber(chapterNumber);
            response.setRating(rating.getRating());
            response.setCreatedAt(rating.getCreatedAt());
            response.setUpdatedAt(rating.getUpdatedAt());

            log.info("User {} rated chapter {} of book {} with {} stars", user.getId(), chapterNumber, slug, request.getRating());

            return new DataResponse<>(SUCCESS, "Rating submitted successfully", HttpStatus.OK.value(), response);
        } catch (Exception e) {
            log.error("Error rating chapter: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public DataResponse<ChapterRatingSummaryResponse> getChapterRatingSummary(String slug, Integer chapterNumber) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);
            if (chapter == null) {
                throw new DataNotFoundException();
            }

            Map<String, Object> summary = ratingMapper.getRatingSummary(book.getId(), chapterNumber);
            List<Map<String, Object>> distribution = ratingMapper.getRatingDistribution(book.getId(), chapterNumber);

            ChapterRatingSummaryResponse response = new ChapterRatingSummaryResponse();
            response.setChapterNumber(chapterNumber);
            response.setChapterTitle(chapter.getTitle());
            response.setAverageRating(safeConvertToDouble(summary.get("average_rating")));
            response.setTotalRatings(safeConvertToInt(summary.get(TOTAL_RATINGS)));

            // Build distribution
            RatingDistribution dist = new RatingDistribution();
            for (Map<String, Object> d : distribution) {
                int rating = safeConvertToInt(d.get("rating"));
                int count = safeConvertToInt(d.get(COUNT));

                switch (rating) {
                    case 1: dist.setOneStar(count); break;
                    case 2: dist.setTwoStars(count); break;
                    case 3: dist.setThreeStars(count); break;
                    case 4: dist.setFourStars(count); break;
                    case 5: dist.setFiveStars(count); break;
                    default: log.warn("Invalid rating value {} found in distribution for chapter {} of book {}", rating, chapterNumber, slug); break;
                }
            }
            response.setDistribution(dist);

            // Get user's rating if authenticated
            Long userId = getCurrentUserIdOrNull();
            if (userId != null) {
                ChapterRating userRating = ratingMapper.findRating(userId, book.getId(), chapterNumber);
                response.setMyRating(userRating != null ? userRating.getRating() : null);
            }

            return new DataResponse<>(SUCCESS, "Rating summary retrieved", HttpStatus.OK.value(), response);
        } catch (Exception e) {
            log.error("Error getting chapter rating summary: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteChapterRating(String slug, Integer chapterNumber) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            ratingMapper.deleteRating(user.getId(), book.getId(), chapterNumber);

            log.info("User {} deleted rating for chapter {} of book {}", user.getId(), chapterNumber, slug);

            return new DataResponse<>(SUCCESS, "Rating deleted", HttpStatus.OK.value(), null);
        } catch (Exception e) {
            log.error("Error deleting chapter rating: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============================================
    // READING ACTIVITY TRACKING
    // ============================================

    @Override
    @Transactional
    public DataResponse<Void> startReading(String slug, StartReadingRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            // Validasi chapter exists
            BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), request.getChapterNumber());
            if (chapter == null) {
                throw new DataNotFoundException();
            }

            // Cek apakah session ini sudah punya activity aktif untuk chapter ini
            ReadingActivityLog existingActivity = activityMapper.findActiveSession(request.getSessionId(), request.getChapterNumber());

            if (existingActivity != null) {
                log.warn("Session {} already has active reading for chapter {}, skipping duplicate", request.getSessionId(), request.getChapterNumber());
                return new DataResponse<>(SUCCESS, "Reading session already active", HttpStatus.OK.value(), null);
            }

            // First time read check (increment book read_count hanya sekali per user)
            int existingSessions = bookMapper.countUserReadSessions(book.getId(), user.getId());
            if (existingSessions == 0) {
                bookMapper.incrementReadCount(book.getId());
                log.info("First time read: User {} started reading book {} (ID: {})", user.getId(), slug, book.getId());
            }

            // Create or get existing session
            ReadingSession session = sessionMapper.findBySessionId(request.getSessionId());
            if (session == null) {
                session = new ReadingSession();
                session.setUserId(user.getId());
                session.setBookId(book.getId());
                session.setSessionId(request.getSessionId());
                session.setStartedAt(LocalDateTime.now());
                session.setStartChapter(request.getChapterNumber());
                session.setDeviceType(request.getDeviceType());
                session.setChaptersRead(0);
                session.setTotalInteractions(0);
                session.setCreatedAt(LocalDateTime.now());
                session.setUpdatedAt(LocalDateTime.now());
                sessionMapper.insertSession(session);
                log.info("Created new reading session: {}", request.getSessionId());
            }

            // Wrap insert dalam try-catch untuk handle race condition di database level
            try {
                ReadingActivityLog activity = new ReadingActivityLog();
                activity.setUserId(user.getId());
                activity.setBookId(book.getId());
                activity.setChapterNumber(request.getChapterNumber());
                activity.setSessionId(request.getSessionId());
                activity.setStartedAt(LocalDateTime.now());
                activity.setStartPosition(request.getStartPosition() != null ? request.getStartPosition() : 0);
                activity.setDeviceType(request.getDeviceType());
                activity.setSource(request.getSource());
                activity.setIsSkip(false);
                activity.setIsReread(false);
                activity.setInteractionCount(0);
                activity.setCreatedAt(LocalDateTime.now());

                activityMapper.insertActivity(activity);

                log.info("User {} started reading chapter {} of book {} (session: {})", user.getId(), request.getChapterNumber(), slug, request.getSessionId());

            } catch (org.springframework.dao.DuplicateKeyException e) {
                // Handle race condition gracefully jika unique constraint triggered
                log.warn("Race condition detected while inserting activity for session {} chapter {}, but it's already handled", request.getSessionId(), request.getChapterNumber());
                return new DataResponse<>(SUCCESS, "Reading session already started (race condition handled)", HttpStatus.OK.value(), null);
            }

            return new DataResponse<>(SUCCESS, "Reading started successfully", HttpStatus.OK.value(), null);

        } catch (DataNotFoundException e) {
            log.error("Resource not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error starting reading for slug {}: {}", slug, e.getMessage(), e);
            throw new RuntimeException("Failed to start reading session", e);
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> endReading(String slug, EndReadingRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            // Validasi chapter exists
            BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), request.getChapterNumber());
            if (chapter == null) {
                throw new DataNotFoundException();
            }

            // Find active reading activity
            ReadingActivityLog activity = activityMapper.findActiveSession(request.getSessionId(), request.getChapterNumber());

            if (activity == null) {
                log.warn("No active reading session found for session {} chapter {}", request.getSessionId(), request.getChapterNumber());
                return new DataResponse<>(SUCCESS, "No active reading session to end", HttpStatus.OK.value(), null);
            }

            // Update activity log with end data
            LocalDateTime now = LocalDateTime.now();
            activity.setEndedAt(now);

            // Calculate duration
            int duration = (int) ChronoUnit.SECONDS.between(activity.getStartedAt(), now);
            activity.setDurationSeconds(duration);

            // Set end metrics
            activity.setEndPosition(request.getEndPosition() != null ? request.getEndPosition() : 0);
            activity.setScrollDepthPercentage(request.getScrollDepthPercentage() != null ? request.getScrollDepthPercentage() : 0.0);
            activity.setWordsRead(request.getWordsRead() != null ? request.getWordsRead() : 0);
            activity.setInteractionCount(request.getInteractionCount() != null ? request.getInteractionCount() : 0);

            // Calculate reading speed (WPM)
            if (activity.getWordsRead() != null && activity.getWordsRead() > 0 && duration > 0) {
                double minutes = duration / 60.0;
                int wpm = (int) (activity.getWordsRead() / minutes);
                activity.setReadingSpeedWpm(wpm);
            }

            // Detect skip behavior (scrolled less than 30%)
            if (activity.getScrollDepthPercentage() != null && activity.getScrollDepthPercentage() < 30.0) {
                activity.setIsSkip(true);
            }

            // Detect reread (check if user has completed this chapter before)
            Integer previousReads = activityMapper.countCompletedReads(user.getId(), book.getId(), request.getChapterNumber(), activity.getId());
            if (previousReads != null && previousReads > 0) {
                activity.setIsReread(true);
            }

            activityMapper.updateActivity(activity);
            log.info("Updated activity log: duration={}s, wpm={}, skip={}, reread={}",
                    duration, activity.getReadingSpeedWpm(), activity.getIsSkip(), activity.getIsReread());

            // Update reading session summary
            updateReadingSession(request.getSessionId(), request.getChapterNumber());

            // Calculate user reading patterns (could be async in production)
            calculateUserReadingPattern(user.getId(), book.getId());

            log.info("User {} ended reading chapter {} of book {}", user.getId(), request.getChapterNumber(), slug);

            return new DataResponse<>(SUCCESS, "Reading ended successfully", HttpStatus.OK.value(), null);

        } catch (DataNotFoundException e) {
            log.error("Resource not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error ending reading for slug {}: {}", slug, e.getMessage(), e);
            throw new RuntimeException("Failed to end reading session", e);
        }
    }

    // Helper: Update reading session summary
    private void updateReadingSession(String sessionId, Integer endChapter) {
        try {
            ReadingSession session = sessionMapper.findBySessionId(sessionId);
            if (session == null) {
                log.warn("Session {} not found, skipping update", sessionId);
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            session.setEndedAt(now);
            session.setEndChapter(endChapter);

            // Calculate total duration
            if (session.getStartedAt() != null) {
                int totalDuration = (int) ChronoUnit.SECONDS.between(session.getStartedAt(), now);
                session.setTotalDurationSeconds(totalDuration);
            }

            // Calculate chapters_read from activity logs
            Integer chaptersRead = activityMapper.countUniqueChaptersInSession(sessionId);
            session.setChaptersRead(chaptersRead != null ? chaptersRead : 0);

            // Calculate total interactions
            Integer totalInteractions = activityMapper.sumInteractionsInSession(sessionId);
            session.setTotalInteractions(totalInteractions != null ? totalInteractions : 0);

            // Calculate completion delta (progress made in this session)
            if (session.getStartChapter() != null && endChapter != null) {
                BookChapter firstChapter = chapterMapper.findChapterByNumber(session.getBookId(), session.getStartChapter());
                BookChapter lastChapter = chapterMapper.findChapterByNumber(session.getBookId(), endChapter);

                if (firstChapter != null && lastChapter != null) {
                    int totalChapters = chapterMapper.countChaptersByBookId(session.getBookId());
                    if (totalChapters > 0) {
                        int chaptersProgressed = Math.abs(endChapter - session.getStartChapter()) + 1;
                        double delta = (chaptersProgressed * 100.0) / totalChapters;
                        session.setCompletionDelta(delta);
                    }
                }
            }

            session.setUpdatedAt(now);
            sessionMapper.updateSession(session);

            log.info("Updated session {}: chapters_read={}, total_duration={}s, completion_delta={}%",
                    sessionId, session.getChaptersRead(), session.getTotalDurationSeconds(),
                    session.getCompletionDelta() != null ? String.format("%.2f", session.getCompletionDelta()) : "N/A");

        } catch (Exception e) {
            log.error("Error updating reading session {}: {}", sessionId, e.getMessage(), e);
            // Don't throw - this is a helper method
        }
    }

    // Helper: Calculate user reading patterns (placeholder for analytics)
    private void calculateUserReadingPattern(Long userId, Long bookId) {
        try {
            // This would ideally run as an async job or event-driven process
            // Analyze reading_activity_log to calculate:
            // - Average reading speed
            // - Preferred reading times
            // - Skip patterns
            // - Completion rates
            // - Engagement levels

            log.info("Calculating reading pattern for user {} and book {}", userId, bookId);

            // Example: Get average WPM for this user on this book
            Integer avgWpm = activityMapper.calculateAverageWpm(userId, bookId);
            if (avgWpm != null) {
                log.debug("User {} average WPM on book {}: {}", userId, bookId, avgWpm);
            }

        } catch (Exception e) {
            log.error("Error calculating reading pattern: {}", e.getMessage(), e);
            // Don't throw - this is analytics, shouldn't break main flow
        }
    }

    // ============================================
    // READING HISTORY & PATTERNS
    // ============================================

    @Override
    public DataResponse<ReadingHistoryResponse> getReadingHistory(String slug) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            // Get activity summary per chapter
            List<Map<String, Object>> summaryData = activityMapper.getUserChapterActivitySummary(user.getId(), book.getId());

            List<ReadingActivitySummary> activities = summaryData.stream()
                    .map(data -> {
                        ReadingActivitySummary summary = new ReadingActivitySummary();
                        summary.setChapterNumber(safeConvertToInt(data.get(CHAPTER_NUMBER)));

                        BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), summary.getChapterNumber());
                        summary.setChapterTitle(chapter != null ? chapter.getTitle() : "");

                        summary.setTimesRead(safeConvertToInt(data.get("times_read")));
                        summary.setTotalReadingTimeSeconds(safeConvertToInt(data.get("total_duration")));
                        summary.setAverageScrollDepth(safeConvertToDouble(data.get("avg_scroll_depth")));
                        summary.setLastReadAt((LocalDateTime) data.get("last_read_at"));
                        summary.setIsCompleted(summary.getAverageScrollDepth() != null && summary.getAverageScrollDepth() > 80);

                        return summary;
                    })
                    .collect(Collectors.toList());

            // Get overall statistics
            Map<String, Object> stats = activityMapper.getUserBookStatistics(user.getId(), book.getId());

            ReadingStatistics statistics = new ReadingStatistics();
            statistics.setTotalChaptersRead(safeConvertToInt(stats.get("chapters_read")));
            statistics.setTotalReadingTimeMinutes(safeConvertToInt(stats.get("total_time")) / 60);
            statistics.setAverageReadingSpeedWpm(safeConvertToInt(stats.get("avg_speed")));

            int totalChapters = book.getTotalPages();
            if (totalChapters > 0) {
                double completionRate = (statistics.getTotalChaptersRead().doubleValue() / totalChapters) * 100;
                statistics.setCompletionRate(completionRate);
            }

            ReadingHistoryResponse response = new ReadingHistoryResponse();
            response.setBookId(book.getId());
            response.setBookTitle(book.getTitle());
            response.setActivities(activities);
            response.setStatistics(statistics);

            return new DataResponse<>(SUCCESS, "Reading history retrieved", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting reading history: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public DataResponse<UserReadingPatternResponse> getUserReadingPattern(String slug) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            UserReadingPattern pattern = patternMapper.findPattern(user.getId(), book.getId());

            if (pattern == null) {
                calculateUserReadingPattern(user.getId(), book.getId());
                pattern = patternMapper.findPattern(user.getId(), book.getId());
            }

            UserReadingPatternResponse response = new UserReadingPatternResponse();
            if (pattern != null) {
                response.setBookId(book.getId());
                response.setBookTitle(book.getTitle());
                response.setPreferredReadingHour(pattern.getPreferredReadingHour());
                response.setPreferredReadingTime(getReadingTimeLabel(pattern.getPreferredReadingHour()));
                response.setPreferredDayOfWeek(pattern.getPreferredDayOfWeek());
                response.setPreferredDay(getDayLabel(pattern.getPreferredDayOfWeek()));
                response.setAverageSessionDurationMinutes(pattern.getAverageSessionDurationMinutes());
                response.setSkipRate(pattern.getSkipRate());
                response.setRereadRate(pattern.getRereadRate());
                response.setCompletionSpeedChaptersPerDay(pattern.getCompletionSpeedChaptersPerDay());
                response.setReadingPace(getReadingPaceLabel(pattern.getCompletionSpeedChaptersPerDay()));
                response.setAnnotationFrequency(pattern.getAnnotationFrequency());
                response.setAnnotationStyle(getAnnotationStyleLabel(pattern.getAnnotationFrequency()));
                response.setAverageReadingSpeedWpm(pattern.getAverageReadingSpeedWpm());
                response.setLastCalculatedAt(pattern.getLastCalculatedAt());
            }

            return new DataResponse<>(SUCCESS, "Reading pattern retrieved", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting reading pattern: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String getReadingTimeLabel(Integer hour) {
        if (hour == null) return UNKNOWN;
        if (hour >= 5 && hour < 12) return "Morning";
        if (hour >= 12 && hour < 17) return "Afternoon";
        if (hour >= 17 && hour < 21) return "Evening";
        return "Night";
    }

    private String getDayLabel(Integer day) {
        if (day == null) return UNKNOWN;
        String[] days = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        return day >= 1 && day <= 7 ? days[day] : UNKNOWN;
    }

    private String getReadingPaceLabel(Double chaptersPerDay) {
        if (chaptersPerDay == null) return UNKNOWN;
        if (chaptersPerDay < 1) return "Slow";
        if (chaptersPerDay < 3) return MODERATE;
        return "Fast";
    }

    private String getAnnotationStyleLabel(Double frequency) {
        if (frequency == null) return UNKNOWN;
        if (frequency < 1) return "Light";
        if (frequency < 3) return MODERATE;
        return "Heavy";
    }

    // ============================================
    // SEARCH IN BOOK
    // ============================================

    @Override
    public DataResponse<SearchInBookResponse> searchInBook(
            String slug, SearchInBookRequest request) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            int offset = (request.getPage() - 1) * request.getLimit();
            String searchQuery = request.getQuery().trim();

            if (searchQuery.isEmpty()) {
                throw new IllegalArgumentException("Search query cannot be empty");
            }

            List<Map<String, Object>> results;
            int totalResults;

            try {
                // Primary: Full-text search
                results = searchMapper.searchInBook(book.getId(), searchQuery, offset, request.getLimit());
                totalResults = searchMapper.countSearchResults(book.getId(), searchQuery);

                log.info("Full-text search returned {} results for query: '{}'", totalResults, searchQuery);
            } catch (Exception e) {
                log.warn("Full-text search failed, falling back to LIKE search: {}", e.getMessage());
                // Fallback: Simple LIKE search
                results = searchMapper.searchInBookSimple(book.getId(), searchQuery, offset, request.getLimit());
                totalResults = searchMapper.countSearchResultsSimple(book.getId(), searchQuery);
            }

            List<ChapterSearchResultResponse> searchResults = results.stream()
                    .map(r -> mapToSearchResult(r, searchQuery))
                    .collect(Collectors.toList());

            SearchInBookResponse response = new SearchInBookResponse();
            response.setQuery(request.getQuery());
            response.setTotalResults(totalResults);
            response.setTotalChapters((int) results.stream()
                    .map(r -> r.get(CHAPTER_NUMBER))
                    .distinct()
                    .count());
            response.setResults(searchResults);

            // Save search history
            saveSearchHistory(getCurrentUserIdOrNull(), book.getId(), request.getQuery(), totalResults);

            return new DataResponse<>(SUCCESS, "Search completed", HttpStatus.OK.value(), response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid search request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error searching in book: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Map database result ke ChapterSearchResultResponse DTO
     */
    private ChapterSearchResultResponse mapToSearchResult(Map<String, Object> result, String query) {

        ChapterSearchResultResponse response = new ChapterSearchResultResponse();

        // Basic chapter info
        response.setChapterId(getLongValue(result));
        response.setChapterNumber(getIntValue(result, CHAPTER_NUMBER));
        response.setChapterTitle((String) result.get(CHAPTER_TITLE));
        response.setChapterSlug((String) result.get("chapter_slug"));
        response.setChapterLevel(getIntValue(result, "chapter_level"));
        response.setParentSlug((String) result.get("parent_slug"));

        // Search relevance
        response.setRelevanceScore(getFloatValue(result));
        response.setMatchCount(getIntValue(result, "match_count"));

        // Extract matches from highlighted content
        String highlightedContent = (String) result.get("highlighted_content");
        String content = (String) result.get("content");

        List<SearchMatch> matches = extractSearchMatches(highlightedContent, content, query);
        response.setMatches(matches);

        return response;
    }

    /**
     * Extract individual search matches dengan context
     */
    private List<SearchMatch> extractSearchMatches(String highlightedContent, String fullContent, String query) {
        List<SearchMatch> matches = new ArrayList<>();

        if (highlightedContent == null || highlightedContent.isEmpty()) {
            return matches;
        }

        // Parse highlighted content untuk extract matches
        // Format: "text <mark>matched</mark> more text"
        Pattern pattern = Pattern.compile("<mark>(.*?)</mark>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(highlightedContent);

        while (matcher.find() && matches.size() < 5) {
            String matchedText = matcher.group(1);

            // Find position in original content
            int position = fullContent.toLowerCase().indexOf(matchedText.toLowerCase());

            if (position >= 0) {
                SearchMatch match = new SearchMatch();
                match.setMatchText(matchedText);
                match.setPosition(position);

                // Extract context
                int contextStart = Math.max(0, position - 50);
                int contextEnd = Math.min(fullContent.length(), position + matchedText.length() + 50);

                match.setContextBefore(fullContent.substring(contextStart, position).trim());
                match.setContextAfter(fullContent.substring(position + matchedText.length(), contextEnd).trim());

                // Create snippet with context
                String snippet = (contextStart > 0 ? "..." : "") +
                        match.getContextBefore() + " " +
                        "<mark>" + matchedText + "</mark>" + " " +
                        match.getContextAfter() +
                        (contextEnd < fullContent.length() ? "..." : "");
                match.setSnippet(snippet.trim());

                matches.add(match);
            }
        }

        // If no matches found from highlighting, create one from content
        if (matches.isEmpty() && fullContent != null) {
            SearchMatch match = createSimpleMatch(fullContent, query);
            if (match != null) {
                matches.add(match);
            }
        }

        return matches;
    }

    /**
     * Create simple match when highlighting tidak tersedia
     */
    private SearchMatch createSimpleMatch(String content, String query) {
        int position = content.toLowerCase().indexOf(query.toLowerCase());

        if (position < 0) {
            return null;
        }

        SearchMatch match = new SearchMatch();
        match.setMatchText(content.substring(position, Math.min(position + query.length(), content.length())));
        match.setPosition(position);

        int contextStart = Math.max(0, position - 50);
        int contextEnd = Math.min(content.length(), position + query.length() + 50);

        match.setContextBefore(content.substring(contextStart, position).trim());
        match.setContextAfter(content.substring(position + query.length(), contextEnd).trim());

        String snippet = (contextStart > 0 ? "..." : "") +
                match.getContextBefore() + " " +
                "<mark>" + match.getMatchText() + "</mark>" + " " +
                match.getContextAfter() +
                (contextEnd < content.length() ? "..." : "");
        match.setSnippet(snippet.trim());

        return match;
    }

    /**
     * Helper methods untuk type conversion
     */
    private Long getLongValue(Map<String, Object> map) {
        Object value = map.get("chapter_id");
        return switch (value) {
            case Long l -> l;
            case Number number -> number.longValue();
            case null, default -> null;
        };
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return switch (value) {
            case Integer i -> i;
            case Number number -> number.intValue();
            case null, default -> null;
        };
    }

    private Float getFloatValue(Map<String, Object> map) {
        Object value = map.get("relevance_score");
        return switch (value) {
            case Float v -> v;
            case Number number -> number.floatValue();
            case null, default -> 0.0f;
        };
    }

    /**
     * Save search history
     */
    private void saveSearchHistory(Long userId, Long bookId, String query, int resultsCount) {
        if (userId != null) {
            try {
                SearchHistory history = new SearchHistory();
                history.setUserId(userId);
                history.setBookId(bookId);
                history.setQuery(query);
                history.setResultsCount(resultsCount);
                history.setSearchType("in_book");
                history.setCreatedAt(LocalDateTime.now());
                searchMapper.insertSearchHistory(history);
            } catch (Exception e) {
                log.warn("Failed to save search history: {}", e.getMessage());
            }
        }
    }

    @Override
    public DataResponse<List<SearchHistoryResponse>> getSearchHistory(String slug, int limit) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            List<SearchHistory> history = searchMapper.getUserBookSearchHistory(
                    user.getId(), book.getId(), limit);

            List<SearchHistoryResponse> responses = history.stream()
                    .map(h -> {
                        SearchHistoryResponse r = new SearchHistoryResponse();
                        r.setId(h.getId());
                        r.setQuery(h.getQuery());
                        r.setResultsCount(h.getResultsCount());
                        r.setSearchedAt(h.getCreatedAt());
                        return r;
                    })
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Search history retrieved",
                    HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error getting search history: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============================================
    // EXPORT ANNOTATIONS
    // ============================================

    @Override
    @Transactional
    public DataResponse<ExportAnnotationsResponse> exportAnnotations(
            String slug, ExportAnnotationsRequest request) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            AnnotationExport export = new AnnotationExport();
            export.setUserId(user.getId());
            export.setBookId(book.getId());
            export.setExportType(request.getExportType());
            export.setIncludeBookmarks(request.getIncludeBookmarks());
            export.setIncludeHighlights(request.getIncludeHighlights());
            export.setIncludeNotes(request.getIncludeNotes());
            export.setChapterFrom(request.getChapterFrom());
            export.setChapterTo(request.getChapterTo());
            export.setDateFrom(request.getDateFrom());
            export.setDateTo(request.getDateTo());
            export.setStatus("PENDING");
            export.setCreatedAt(LocalDateTime.now());
            export.setExpiresAt(LocalDateTime.now().plusDays(7));

            exportMapper.insertExport(export);

            // TODO: Trigger async job
            processExport(export, user, book);

            ExportAnnotationsResponse response = mapToExportResponse(export);

            return new DataResponse<>(SUCCESS, "Export started",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error exporting annotations: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void processExport(AnnotationExport export, User user, Book book) {
        // TODO: Implement actual export generation
        // This should be an async job that:
        // 1. Fetches all annotations based on filters
        // 2. Generates the file (PDF/DOCX/etc)
        // 3. Uploads to S3
        // 4. Updates export record with URL

        export.setStatus("PROCESSING");
        exportMapper.updateExport(export);

        // Simulate processing...
        log.info("Processing export {} for user {}", export.getId(), user.getId());
    }

    @Override
    public DataResponse<ExportAnnotationsResponse> getExportStatus(Long exportId) {
        try {
            AnnotationExport export = exportMapper.findById(exportId);
            if (export == null) {
                throw new DataNotFoundException();
            }

            ExportAnnotationsResponse response = mapToExportResponse(export);
            return new DataResponse<>(SUCCESS, "Export status retrieved",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting export status: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getExportDownloadUrl(Long exportId) {
        AnnotationExport export = exportMapper.findById(exportId);
        if (export == null || export.getFileUrl() == null) {
            throw new DataNotFoundException();
        }
        return export.getFileUrl();
    }

    @Override
    public DataResponse<ExportHistoryResponse> getExportHistory(int page, int limit) {
        try {
            User user = getCurrentUser();

            int offset = (page - 1) * limit;
            List<AnnotationExport> exports = exportMapper.findUserExports(
                    user.getId(), offset, limit);

            List<ExportAnnotationsResponse> responses = exports.stream()
                    .map(this::mapToExportResponse)
                    .collect(Collectors.toList());

            ExportHistoryResponse response = new ExportHistoryResponse();
            response.setExports(responses);
            response.setTotalExports(responses.size());
            response.setTotalBytesExported(
                    responses.stream()
                            .filter(r -> r.getFileSize() != null)
                            .mapToLong(ExportAnnotationsResponse::getFileSize)
                            .sum());

            return new DataResponse<>(SUCCESS, "Export history retrieved",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting export history: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteExport(Long exportId) {
        try {
            User user = getCurrentUser();
            AnnotationExport export = exportMapper.findById(exportId);

            if (export == null) {
                throw new DataNotFoundException();
            }

            if (!export.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

//            exportMapper.deleteExport(exportId);

            return new DataResponse<>(SUCCESS, "Export deleted",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting export: {}", e.getMessage(), e);
            throw e;
        }
    }

// ============================================
    // 🔥 BULK USER DATA - MOST IMPORTANT FOR DASHBOARD
    // ============================================

    @Override
    @Cacheable(value = "user-book-data", key = "#slug + ':' + T(com.naskah.demo.util.interceptor.HeaderHolder).getUsername()")
    public DataResponse<UserBookDataResponse> getMyBookData(String slug) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            UserBookDataResponse response = new UserBookDataResponse();

            // 1. Reading Progress
            response.setReadingProgress(buildReadingProgressSummary(user.getId(), book));

            // 2. Annotations
            response.setBookmarks(getBookmarksList(user.getId(), book.getId()));
            response.setHighlights(getHighlightsList(user.getId(), book.getId()));
            response.setNotes(getNotesList(user.getId(), book.getId()));

            // 3. Ratings
            List<ChapterRating> userRatings = ratingMapper.findUserBookRatings(user.getId(), book.getId());
            response.setRatings(userRatings.stream()
                    .map(this::mapToRatingResponse)
                    .collect(Collectors.toList()));

            if (!userRatings.isEmpty()) {
                double avgRating = userRatings.stream()
                        .mapToInt(ChapterRating::getRating)
                        .average()
                        .orElse(0.0);
                response.setMyAverageRating(avgRating);
            }

            // 4. Reading History
            response.setReadingHistory(buildReadingHistorySummary(user.getId(), book.getId()));

            // 5. Search History
            response.setRecentSearches(getRecentSearches(user.getId(), book.getId()));

            // 6. Reading Patterns
            UserReadingPattern pattern = patternMapper.findPattern(user.getId(), book.getId());
            if (pattern != null) {
                response.setPatterns(mapToPatternResponse(pattern, book));
            }

            // 7. Statistics
            response.setStatistics(calculateUserBookStatistics(response));

            log.info("Retrieved complete book data for user {} and book {}", user.getId(), slug);

            return new DataResponse<>(SUCCESS, "User book data retrieved",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting user book data: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============================================
    // ANALYTICS FOR AUTHORS
    // ============================================

    @Override
    @Cacheable(value = "book-analytics", key = "#slug + ':' + #dateFrom + ':' + #dateTo")
    public DataResponse<BookAnalyticsResponse> getBookAnalytics(
            String slug, String dateFrom, String dateTo) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            // TODO: Check if user is author/admin

            LocalDateTime startDate = parseDate(dateFrom);
            LocalDateTime endDate = parseDate(dateTo);

            BookAnalyticsResponse response = new BookAnalyticsResponse();
            response.setBookId(book.getId());
            response.setBookTitle(book.getTitle());
            response.setAnalyzedAt(LocalDateTime.now());
            response.setDateRange(formatDateRange(startDate, endDate));

            response.setOverview(buildAnalyticsOverview(book.getId(), startDate, endDate));
            response.setReaderBehavior(buildReaderBehaviorAnalytics(book.getId(), startDate, endDate));
            response.setContentEngagement(buildContentEngagementAnalytics(book.getId()));
            response.setMostHighlightedPassages(findMostHighlightedPassages(book.getId()));
            response.setDropOffPoints(findDropOffPoints(book.getId()));
            response.setMostSkippedChapters(findMostSkippedChapters(book.getId()));
            response.setTrends(analyzeTrends(book.getId(), startDate, endDate));

            return new DataResponse<>(SUCCESS, "Analytics retrieved",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting book analytics: {}", e.getMessage(), e);
            throw e;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    private String formatDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return "All time";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        if (start == null) {
            return "Until " + end.format(formatter);
        }
        if (end == null) {
            return "From " + start.format(formatter);
        }

        return start.format(formatter) + " - " + end.format(formatter);
    }


    @Override
    @Cacheable(value = "chapters-analytics", key = "#slug")
    public DataResponse<List<ChapterAnalyticsResponse>> getChaptersAnalytics(String slug) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            List<BookChapter> chapters = chapterMapper.findChaptersByBookId(book.getId());

            List<ChapterAnalyticsResponse> analytics = chapters.stream()
                    .map(chapter -> buildChapterAnalytics(book, chapter))
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Chapter analytics retrieved",
                    HttpStatus.OK.value(), analytics);

        } catch (Exception e) {
            log.error("Error getting chapters analytics: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void validateBook(Book book) {
        if (book == null) {
            throw new DataNotFoundException();
        }
    }

    private User getCurrentUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new UnauthorizedException();
        }

        User user = userMapper.findUserByUsername(username);
        if (user == null) {
            throw new UnauthorizedException();
        }

        return user;
    }

    private Long getCurrentUserIdOrNull() {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.trim().isEmpty()) {
                return null;
            }

            User user = userMapper.findUserByUsername(username);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================
// HELPER METHODS - Chapter Operations
// ============================================

    private BookChapter findChapterBySlugHierarchy(Long bookId, String[] slugParts) {
        Long currentParentId = null;
        BookChapter currentChapter = null;

        for (String slug : slugParts) {
            currentChapter = chapterMapper.findChapterBySlugAndParent(bookId, slug, currentParentId);

            if (currentChapter == null) {
                return null;
            }

            currentParentId = currentChapter.getId();
        }

        return currentChapter;
    }

    // ✅ TAMBAH METHOD INI - Build full path dari chapter hierarchy
    private String buildFullChapterPath(BookChapter chapter) {
        if (chapter == null) {
            return "";
        }

        List<String> pathSegments = new ArrayList<>();
        BookChapter current = chapter;

        // Build path from bottom to top
        while (current != null) {
            pathSegments.add(0, current.getSlug());

            if (current.getParentChapterId() != null) {
                current = chapterMapper.findChapterById(current.getParentChapterId());
            } else {
                current = null;
            }
        }

        // Join with "/"
        return String.join("/", pathSegments);
    }

    private List<ChapterBreadcrumb> buildBreadcrumbs(BookChapter chapter) {
        List<ChapterBreadcrumb> breadcrumbs = new ArrayList<>();
        List<BookChapter> hierarchy = new ArrayList<>();
        BookChapter current = chapter;

        // Collect all chapters in hierarchy
        while (current != null) {
            hierarchy.add(0, current);

            if (current.getParentChapterId() != null) {
                current = chapterMapper.findChapterById(current.getParentChapterId());
            } else {
                current = null;
            }
        }

        // Build breadcrumbs with full path for each level
        List<String> pathSegments = new ArrayList<>();
        for (BookChapter ch : hierarchy) {
            pathSegments.add(ch.getSlug());

            ChapterBreadcrumb breadcrumb = new ChapterBreadcrumb();
            breadcrumb.setChapterId(ch.getId());
            breadcrumb.setTitle(ch.getTitle());
            breadcrumb.setSlug(ch.getSlug());
            breadcrumb.setChapterLevel(ch.getChapterLevel());
            breadcrumb.setFullPath(String.join("/", pathSegments)); // ✅ Set full path

            breadcrumbs.add(breadcrumb);
        }

        return breadcrumbs;
    }

    private void setChapterNavigation(ChapterReadingResponse response,
                                      Long bookId, BookChapter currentChapter) {

        // Previous
        BookChapter prevChapter = chapterMapper.findChapterByNumber(bookId, currentChapter.getChapterNumber() - 1);
        if (prevChapter != null) {
            response.setPreviousChapter(mapToNavigationInfo(prevChapter));
        }

        // Next
        BookChapter nextChapter = chapterMapper.findChapterByNumber(bookId, currentChapter.getChapterNumber() + 1);
        if (nextChapter != null) {
            response.setNextChapter(mapToNavigationInfo(nextChapter));
        }

        // Parent
        if (currentChapter.getParentChapterId() != null) {
            BookChapter parentChapter = chapterMapper.findChapterById(currentChapter.getParentChapterId());
            if (parentChapter != null) {
                response.setParentChapter(mapToNavigationInfo(parentChapter));
            }
        }
    }

    private ChapterNavigationInfo mapToNavigationInfo(BookChapter chapter) {
        ChapterNavigationInfo info = new ChapterNavigationInfo();
        info.setChapterNumber(chapter.getChapterNumber());
        info.setTitle(chapter.getTitle());
        info.setChapterLevel(chapter.getChapterLevel());
        info.setSlug(chapter.getSlug());

        // ✅ Build full path
        info.setFullPath(buildFullChapterPath(chapter));

        // Set parent slug (for backward compatibility)
        if (chapter.getParentChapterId() != null) {
            BookChapter parent = chapterMapper.findChapterById(chapter.getParentChapterId());
            if (parent != null) {
                info.setParentSlug(parent.getSlug());
            }
        }

        return info;
    }

    private List<ChapterSummaryResponse> buildChapterHierarchy(
            List<BookChapter> chapters, Long bookId, Long userId) {

        Map<Long, ChapterSummaryResponse> chapterMap = new HashMap<>();
        Map<Long, BookChapter> chapterEntityMap = new HashMap<>();
        List<ChapterSummaryResponse> rootChapters = new ArrayList<>();

        // First pass: create map of entities
        for (BookChapter chapter : chapters) {
            chapterEntityMap.put(chapter.getId(), chapter);
        }

        // Second pass: build responses
        for (BookChapter chapter : chapters) {
            ChapterSummaryResponse response = new ChapterSummaryResponse();
            response.setId(chapter.getId());
            response.setChapterNumber(chapter.getChapterNumber());
            response.setParentChapterId(chapter.getParentChapterId());
            response.setChapterLevel(chapter.getChapterLevel());
            response.setTitle(chapter.getTitle());
            response.setSlug(chapter.getSlug());
            response.setWordCount(chapter.getWordCount());
            response.setEstimatedReadTime(calculateReadTime(chapter.getWordCount()));
            response.setSubChapters(new ArrayList<>());

            // ✅ Build full path
            response.setFullPath(buildFullChapterPath(chapter));

            if (userId != null) {
                ChapterProgress progress = chapterProgressMapper.findProgress(userId, bookId, chapter.getChapterNumber());
                response.setIsCompleted(progress != null && progress.getIsCompleted());
            } else {
                response.setIsCompleted(false);
            }

            chapterMap.put(chapter.getId(), response);
        }

        // Build hierarchy
        for (BookChapter chapter : chapters) {
            ChapterSummaryResponse response = chapterMap.get(chapter.getId());

            if (chapter.getParentChapterId() == null) {
                rootChapters.add(response);
            } else {
                ChapterSummaryResponse parent = chapterMap.get(chapter.getParentChapterId());
                if (parent != null) {
                    parent.getSubChapters().add(response);
                } else {
                    rootChapters.add(response);
                }
            }
        }

        return rootChapters;
    }
    // ============================================
    // HELPER METHODS - Annotations
    // ============================================

    private List<BookmarkResponse> getUserChapterBookmarks(Long userId, Long bookId, Integer chapterNumber) {
        List<Bookmark> bookmarks;

        if (chapterNumber != null) {
            bookmarks = bookmarkMapper.findByUserBookAndChapter(userId, bookId, chapterNumber);
        } else {
            bookmarks = bookmarkMapper.findByUserAndBook(userId, bookId);
        }

        return bookmarks.stream()
                .map(entityMapper::toBookmarkResponse)
                .collect(Collectors.toList());
    }

    private List<HighlightResponse> getUserChapterHighlights(Long userId, Long bookId, Integer chapterNumber) {
        List<Highlight> highlights;

        if (chapterNumber != null) {
            highlights = highlightMapper.findByUserBookAndChapter(userId, bookId, chapterNumber);
        } else {
            highlights = highlightMapper.findByUserAndBook(userId, bookId);
        }

        return highlights.stream()
                .map(entityMapper::toHighlightResponse)
                .collect(Collectors.toList());
    }

    private List<NoteResponse> getUserChapterNotes(Long userId, Long bookId, Integer chapterNumber) {
        List<Note> notes;

        if (chapterNumber != null) {
            notes = noteMapper.findByUserBookAndChapter(userId, bookId, chapterNumber);
        } else {
            notes = noteMapper.findByUserAndBook(userId, bookId);
        }

        return notes.stream()
                .map(entityMapper::toNoteResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // TEXT-TO-SPEECH SUPPORT
    // ============================================

    @Override
    @Cacheable(value = "chapter-text", key = "#slug + ':' + #chapterNumber")
    public DataResponse<ChapterTextResponse> getChapterTextForTTS(String slug, Integer chapterNumber) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);
            if (chapter == null) {
                throw new DataNotFoundException();
            }

            String plainText = extractTextFromHtml(chapter.getHtmlContent());

            ChapterTextResponse response = new ChapterTextResponse();
            response.setChapterNumber(chapter.getChapterNumber());
            response.setChapterTitle(chapter.getTitle());
            response.setPlainText(plainText);
            response.setWordCount(chapter.getWordCount());
            response.setEstimatedDuration(calculateReadTime(chapter.getWordCount()) * 60);

            return new DataResponse<>(SUCCESS, "Chapter text retrieved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting chapter text for TTS: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String extractTextFromHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    @Override
    @Cacheable(value = "chapter-paragraphs", key = "#slug + ':' + #chapterNumber")
    public DataResponse<ChapterParagraphsResponse> getChapterParagraphs(String slug, Integer chapterNumber) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);
            if (chapter == null) {
                throw new DataNotFoundException();
            }

            org.jsoup.nodes.Document doc = Jsoup.parse(chapter.getHtmlContent());
            List<String> paragraphs = doc.select("p").stream()
                    .map(org.jsoup.nodes.Element::text)
                    .filter(text -> !text.trim().isEmpty())
                    .collect(Collectors.toList());

            ChapterParagraphsResponse response = new ChapterParagraphsResponse();
            response.setChapterNumber(chapter.getChapterNumber());
            response.setChapterTitle(chapter.getTitle());
            response.setParagraphs(paragraphs);
            response.setTotalParagraphs(paragraphs.size());

            return new DataResponse<>(SUCCESS, "Chapter paragraphs retrieved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting chapter paragraphs: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "chapter-stats", key = "#slug + ':' + #chapterNumber")
    public DataResponse<ChapterStatsResponse> getChapterStats(String slug, Integer chapterNumber) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);
            if (chapter == null) {
                throw new DataNotFoundException();
            }

            ChapterStatsResponse response = new ChapterStatsResponse();
            response.setChapterNumber(chapterNumber);
            response.setChapterTitle(chapter.getTitle());

            Map<String, Object> stats = analyticsMapper.getChapterStats(book.getId(), chapterNumber);

            response.setReaderCount(safeConvertToInt(stats.get("reader_count")));
            response.setCompletionRate(safeConvertToDouble(stats.get("completion_rate")));
            response.setAverageReadingTimeMinutes(safeConvertToInt(stats.get(AVG_READING_TIME)));
            response.setHighlightCount(safeConvertToInt(stats.get(HIGHLIGHT_COUNT)));
            response.setNoteCount(safeConvertToInt(stats.get("note_count")));
            response.setCommentCount(safeConvertToInt(stats.get("comment_count")));

            return new DataResponse<>(SUCCESS, "Statistics retrieved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting chapter stats: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "chapter-annotations", key = "#slug")
    public DataResponse<ChapterAnnotationsResponse> getMyChapterAnnotations(String slug) {
        try {
            User user = getCurrentUser();
            Book book = bookMapper.findBookBySlug(slug);
            validateBook(book);

            ChapterAnnotationsResponse response = new ChapterAnnotationsResponse();
            response.setBookmarks(getUserChapterBookmarks(user.getId(), book.getId(), null));
            response.setHighlights(getUserChapterHighlights(user.getId(), book.getId(), null));
            response.setNotes(getUserChapterNotes(user.getId(), book.getId(), null));

            return new DataResponse<>(SUCCESS, "Annotations retrieved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting chapter annotations: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============================================
    // HELPER METHODS - Type Conversion
    // ============================================

    private Integer safeConvertToInt(Object value) {
        switch (value) {
            case null -> {
                return 0;
            }
            case Integer i -> {
                return i;
            }
            case Long l -> {
                return l.intValue();
            }
            case BigDecimal bigDecimal -> {
                return bigDecimal.intValue();
            }
            case Number number -> {
                return number.intValue();
            }
            case String ignored -> {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            default -> {
            }
        }
        return 0;
    }

    private Long safeConvertToLong(Object value) {
        switch (value) {
            case null -> {
                return 0L;
            }
            case Long l -> {
                return l;
            }
            case Integer i -> {
                return i.longValue();
            }
            case BigDecimal bigDecimal -> {
                return bigDecimal.longValue();
            }
            case Number number -> {
                return number.longValue();
            }
            case String ignored -> {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    return 0L;
                }
            }
            default -> {
            }
        }
        return 0L;
    }

    private Double safeConvertToDouble(Object value) {
        switch (value) {
            case null -> {
                return 0.0;
            }
            case Double v -> {
                return v;
            }
            case Float v -> {
                return v.doubleValue();
            }
            case BigDecimal bigDecimal -> {
                return bigDecimal.doubleValue();
            }
            case Number number -> {
                return number.doubleValue();
            }
            case String ignored -> {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
            default -> {
            }
        }
        return 0.0;
    }

    // ============================================
    // HELPER METHODS - Reviews & Ratings
    // ============================================

    private ChapterReviewResponse mapToChapterReviewResponse(ChapterReview review, Long currentUserId) {
        User reviewUser = userMapper.findUserById(review.getUserId());

        ChapterReviewResponse response = new ChapterReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setUserName(reviewUser != null ? reviewUser.getUsername() : UNKNOWN);
        response.setUserProfilePicture(reviewUser != null ? reviewUser.getProfilePictureUrl() : null);
        response.setChapterNumber(review.getChapterNumber());
        response.setComment(review.getContent());
        response.setIsSpoiler(review.getIsSpoiler());
        response.setLikeCount(review.getLikeCount());
        response.setParentId(review.getParentId());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        if (currentUserId != null) {
            boolean isLiked = chapterReviewMapper.isReviewLikedByUser(review.getId(), currentUserId);
            response.setIsLikedByMe(isLiked);
        } else {
            response.setIsLikedByMe(false);
        }

        if (review.getParentId() == null) {
            List<ChapterReview> replies = chapterReviewMapper.findReplies(review.getId());
            response.setReplies(replies.stream()
                    .map(r -> mapToChapterReviewResponse(r, currentUserId))
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private ChapterRatingResponse mapToRatingResponse(ChapterRating rating) {
        ChapterRatingResponse response = new ChapterRatingResponse();
        response.setId(rating.getId());
        response.setUserId(rating.getUserId());
        response.setChapterNumber(rating.getChapterNumber());
        response.setRating(rating.getRating());
        response.setCreatedAt(rating.getCreatedAt());
        response.setUpdatedAt(rating.getUpdatedAt());
        return response;
    }

    // ============================================
    // HELPER METHODS - Bulk Data Building
    // ============================================

    private ReadingProgressSummary buildReadingProgressSummary(Long userId, Book book) {
        ReadingProgressSummary summary = new ReadingProgressSummary();

        List<ChapterProgress> allProgress = chapterProgressMapper.findAllByUserAndBook(userId, book.getId());

        allProgress.stream()
                .filter(p -> !p.getIsCompleted())
                .max(Comparator.comparing(ChapterProgress::getLastReadAt)).ifPresent(lastIncomplete -> summary.setCurrentChapter(lastIncomplete.getChapterNumber()));

        summary.setTotalChapters(book.getTotalPages());

        long completedCount = allProgress.stream()
                .filter(ChapterProgress::getIsCompleted)
                .count();
        double completion = (completedCount * 100.0) / book.getTotalPages();
        summary.setCompletionPercentage(completion);

        summary.setLastReadAt(allProgress.stream()
                .map(ChapterProgress::getLastReadAt)
                .max(Comparator.naturalOrder())
                .orElse(null));

        int totalTime = allProgress.stream().mapToInt(ChapterProgress::getReadingTimeSeconds).sum() / 60;
        summary.setTotalReadingTimeMinutes(totalTime);

        summary.setCompletedChapters(allProgress.stream()
                .filter(ChapterProgress::getIsCompleted)
                .map(ChapterProgress::getChapterNumber)
                .collect(Collectors.toList()));

        summary.setChaptersInProgress((int) allProgress.stream().filter(p -> !p.getIsCompleted() && p.getReadingTimeSeconds() > 0).count());

        return summary;
    }

    private ReadingHistorySummary buildReadingHistorySummary(Long userId, Long bookId) {
        ReadingHistorySummary summary = new ReadingHistorySummary();

        List<ReadingSession> sessions = sessionMapper.findUserBookSessions(userId, bookId, 0, 1000);

        summary.setTotalSessions(sessions.size());

        int totalTime = sessions.stream()
                .mapToInt(s -> s.getTotalDurationSeconds() != null ? s.getTotalDurationSeconds() : 0)
                .sum() / 60;
        summary.setTotalReadingTimeMinutes(totalTime);

        summary.setFirstReadAt(sessions.stream()
                .map(ReadingSession::getStartedAt)
                .min(Comparator.naturalOrder())
                .orElse(null));

        summary.setLastReadAt(sessions.stream()
                .map(ReadingSession::getStartedAt)
                .max(Comparator.naturalOrder())
                .orElse(null));

        summary.setCurrentStreak(calculateCurrentStreak(sessions));
        summary.setLongestStreak(calculateLongestStreak(sessions));

        return summary;
    }

    private int calculateLongestStreak(List<ReadingSession> sessions) {
        if (sessions.isEmpty()) return 0;

        List<LocalDateTime> dates = sessions.stream()
                .map(ReadingSession::getStartedAt)
                .map(dt -> dt.toLocalDate().atStartOfDay())
                .distinct()
                .sorted()
                .toList();

        int maxStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < dates.size(); i++) {
            long daysBetween = java.time.Duration.between(dates.get(i-1), dates.get(i)).toDays();

            if (daysBetween == 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }

    private UserBookStatistics calculateUserBookStatistics(UserBookDataResponse data) {
        UserBookStatistics stats = new UserBookStatistics();

        stats.setTotalBookmarks(data.getBookmarks().size());
        stats.setTotalHighlights(data.getHighlights().size());
        stats.setTotalNotes(data.getNotes().size());
        stats.setTotalRatings(data.getRatings().size());
        stats.setTotalSearches(data.getRecentSearches().size());
        stats.setTotalReadingSessions(data.getReadingHistory().getTotalSessions());

        double score = 0;
        score += Math.min(stats.getTotalBookmarks() * 2, 20);
        score += Math.min(stats.getTotalHighlights() * 1.5, 20);
        score += Math.min(stats.getTotalNotes() * 3, 20);
        score += Math.min(stats.getTotalRatings() * 2, 20);
        score += Math.min(data.getReadingProgress().getCompletionPercentage() * 0.2, 20);

        stats.setEngagementScore(score);

        return stats;
    }

    private List<BookmarkResponse> getBookmarksList(Long userId, Long bookId) {
        return bookmarkMapper.findByUserAndBook(userId, bookId).stream()
                .map(entityMapper::toBookmarkResponse)
                .collect(Collectors.toList());
    }

    private List<HighlightResponse> getHighlightsList(Long userId, Long bookId) {
        return highlightMapper.findByUserAndBook(userId, bookId).stream()
                .map(entityMapper::toHighlightResponse)
                .collect(Collectors.toList());
    }

    private List<NoteResponse> getNotesList(Long userId, Long bookId) {
        return noteMapper.findByUserAndBook(userId, bookId).stream()
                .map(entityMapper::toNoteResponse)
                .collect(Collectors.toList());
    }

    private List<SearchHistoryResponse> getRecentSearches(Long userId, Long bookId) {
        return searchMapper.getUserBookSearchHistory(userId, bookId, 10).stream()
                .map(s -> {
                    SearchHistoryResponse r = new SearchHistoryResponse();
                    r.setId(s.getId());
                    r.setQuery(s.getQuery());
                    r.setResultsCount(s.getResultsCount());
                    r.setSearchedAt(s.getCreatedAt());
                    r.setWasClicked(s.getClickedAt() != null);
                    return r;
                })
                .collect(Collectors.toList());
    }

    private UserReadingPatternResponse mapToPatternResponse(UserReadingPattern pattern, Book book) {
        UserReadingPatternResponse response = new UserReadingPatternResponse();
        response.setBookId(book.getId());
        response.setBookTitle(book.getTitle());
        response.setPreferredReadingHour(pattern.getPreferredReadingHour());
        response.setPreferredReadingTime(getReadingTimeLabel(pattern.getPreferredReadingHour()));
        response.setPreferredDayOfWeek(pattern.getPreferredDayOfWeek());
        response.setPreferredDay(getDayLabel(pattern.getPreferredDayOfWeek()));
        response.setAverageSessionDurationMinutes(pattern.getAverageSessionDurationMinutes());
        response.setSkipRate(pattern.getSkipRate());
        response.setRereadRate(pattern.getRereadRate());
        response.setCompletionSpeedChaptersPerDay(pattern.getCompletionSpeedChaptersPerDay());
        response.setReadingPace(getReadingPaceLabel(pattern.getCompletionSpeedChaptersPerDay()));
        response.setAnnotationFrequency(pattern.getAnnotationFrequency());
        response.setAnnotationStyle(getAnnotationStyleLabel(pattern.getAnnotationFrequency()));
        response.setAverageReadingSpeedWpm(pattern.getAverageReadingSpeedWpm());
        response.setLastCalculatedAt(pattern.getLastCalculatedAt());
        return response;
    }

    // ============================================
    // HELPER METHODS - Analytics Building
    // ============================================

    private AnalyticsOverview buildAnalyticsOverview(Long bookId, LocalDateTime startDate, LocalDateTime endDate) {
        AnalyticsOverview overview = new AnalyticsOverview();

        Map<String, Object> metrics = analyticsMapper.getBookOverviewMetrics(bookId, startDate, endDate);

        overview.setTotalReaders(safeConvertToLong(metrics.get("total_readers")));
        overview.setActiveReaders(safeConvertToLong(metrics.get("active_readers")));
        overview.setNewReaders(safeConvertToLong(metrics.get("new_readers")));
        overview.setAverageCompletionRate(safeConvertToDouble(metrics.get("avg_completion_rate")));
        overview.setAverageReadingTimeMinutes(safeConvertToInt(metrics.get(AVG_READING_TIME)));
        overview.setAverageRating(safeConvertToDouble(metrics.get("avg_rating")));
        overview.setTotalRatings(safeConvertToInt(metrics.get(TOTAL_RATINGS)));
        overview.setTotalReviews(safeConvertToInt(metrics.get("total_reviews")));

        return overview;
    }

    private ReaderBehaviorAnalytics buildReaderBehaviorAnalytics(Long bookId, LocalDateTime startDate, LocalDateTime endDate) {
        ReaderBehaviorAnalytics behavior = new ReaderBehaviorAnalytics();

        List<Map<String, Object>> deviceData = analyticsMapper.getReadersByDevice(bookId, startDate, endDate);
        behavior.setReadersByDevice(deviceData.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("device_type"),
                        m -> safeConvertToInt(m.get(COUNT))
                )));

        List<Map<String, Object>> hourData = analyticsMapper.getReadingHourDistribution(bookId, startDate, endDate);
        behavior.setReadingHourDistribution(hourData.stream()
                .collect(Collectors.toMap(
                        m -> safeConvertToInt(m.get("hour")),
                        m -> safeConvertToInt(m.get(COUNT))
                )));

        Map<String, Object> patterns = analyticsMapper.getReadingPatterns(bookId, startDate, endDate);
        behavior.setAverageSessionDurationMinutes(safeConvertToDouble(patterns.get("avg_session_duration")));
        behavior.setAverageReadingSpeedWpm(safeConvertToDouble(patterns.get("avg_reading_speed")));
        behavior.setAverageChaptersPerSession(safeConvertToInt(patterns.get("avg_chapters_per_session")));

        Map<String, Object> engagement = analyticsMapper.getEngagementRates(bookId, startDate, endDate);
        behavior.setAnnotationRate(safeConvertToDouble(engagement.get("annotation_rate")));
        behavior.setRatingRate(safeConvertToDouble(engagement.get("rating_rate")));
        behavior.setReviewRate(safeConvertToDouble(engagement.get("review_rate")));

        return behavior;
    }

    private ContentEngagementAnalytics buildContentEngagementAnalytics(Long bookId) {
        ContentEngagementAnalytics engagement = new ContentEngagementAnalytics();

        Map<String, Object> counts = analyticsMapper.getAnnotationCounts(bookId);

        engagement.setTotalAnnotations(safeConvertToInt(counts.get("total_annotations")));
        engagement.setTotalBookmarks(safeConvertToInt(counts.get("total_bookmarks")));
        engagement.setTotalHighlights(safeConvertToInt(counts.get("total_highlights")));
        engagement.setTotalNotes(safeConvertToInt(counts.get("total_notes")));

        List<Map<String, Object>> topChapters = analyticsMapper.getTopEngagedChapters(bookId, 10);
        engagement.setTopEngagedChapters(topChapters.stream()
                .map(this::mapToChapterEngagement)
                .collect(Collectors.toList()));

        int totalChapters = chapterMapper.countChaptersByBookId(bookId);
        if (totalChapters > 0) {
            engagement.setAverageAnnotationsPerChapter(engagement.getTotalAnnotations() / (double) totalChapters);
        }

        return engagement;
    }

    private List<PopularPassage> findMostHighlightedPassages(Long bookId) {
        List<Map<String, Object>> data = analyticsMapper.getMostHighlightedPassages(bookId, 10);

        return data.stream()
                .map(this::mapToPopularPassage)
                .collect(Collectors.toList());
    }

    private List<ChapterDropOffPoint> findDropOffPoints(Long bookId) {
        List<Map<String, Object>> data = analyticsMapper.getDropOffPoints(bookId);

        return data.stream()
                .map(this::mapToDropOffPoint)
                .filter(d -> d.getDropOffRate() > 20.0)
                .sorted(Comparator.comparing(ChapterDropOffPoint::getDropOffRate).reversed())
                .collect(Collectors.toList());
    }

    private List<ChapterSkipAnalysis> findMostSkippedChapters(Long bookId) {
        List<Map<String, Object>> data = analyticsMapper.getMostSkippedChapters(bookId);

        return data.stream()
                .map(d -> {
                    ChapterSkipAnalysis analysis = new ChapterSkipAnalysis();
                    analysis.setChapterNumber(safeConvertToInt(d.get(CHAPTER_NUMBER)));
                    analysis.setChapterTitle((String) d.get(CHAPTER_TITLE));
                    analysis.setSkipRate(safeConvertToDouble(d.get("skip_rate")));
                    analysis.setTimesSkipped(safeConvertToInt(d.get("times_skipped")));
                    analysis.setTotalReaders(safeConvertToInt(d.get("total_readers")));
                    analysis.setPossibleReason(inferSkipReason(analysis));
                    return analysis;
                })
                .collect(Collectors.toList());
    }

    private TrendAnalysis analyzeTrends(Long bookId, LocalDateTime startDate, LocalDateTime endDate) {
        TrendAnalysis trends = new TrendAnalysis();

        LocalDateTime previousStart = startDate.minusDays(java.time.Duration.between(startDate, endDate).toDays());

        Map<String, Object> currentMetrics = analyticsMapper.getBookOverviewMetrics(bookId, startDate, endDate);
        Map<String, Object> previousMetrics = analyticsMapper.getBookOverviewMetrics(bookId, previousStart, startDate);

        long currentReaders = safeConvertToLong(currentMetrics.get("total_readers"));
        long previousReaders = safeConvertToLong(previousMetrics.get("total_readers"));

        if (previousReaders > 0) {
            int readerChange = (int) (((currentReaders - previousReaders) * 100.0) / previousReaders);
            trends.setReadersChangePercentage(readerChange);

            if (readerChange > 10) trends.setReadersGrowth("increasing");
            else if (readerChange < -10) trends.setReadersGrowth("decreasing");
            else trends.setReadersGrowth("stable");
        }

        trends.setEngagementTrend("stable");
        trends.setPopularityTrend("stable");

        if (currentReaders > previousReaders) {
            long growth = currentReaders - previousReaders;
            trends.setEstimatedReadersNextMonth((int) (currentReaders + growth));
        } else {
            trends.setEstimatedReadersNextMonth((int) currentReaders);
        }

        double currentCompletion = safeConvertToDouble(currentMetrics.get("avg_completion_rate"));
        trends.setEstimatedCompletionRate(currentCompletion);

        return trends;
    }

    private ChapterAnalyticsResponse buildChapterAnalytics(Book book, BookChapter chapter) {
        ChapterAnalyticsResponse analytics = new ChapterAnalyticsResponse();

        analytics.setChapterNumber(chapter.getChapterNumber());
        analytics.setChapterTitle(chapter.getTitle());
        analytics.setChapterSlug(chapter.getSlug());

        Map<String, Object> stats = analyticsMapper.getChapterStats(book.getId(), chapter.getChapterNumber());

        analytics.setTotalReaders(safeConvertToLong(stats.get("total_readers")));
        analytics.setUniqueReaders(safeConvertToLong(stats.get("unique_readers")));
        analytics.setAverageReadingTimeSeconds(safeConvertToInt(stats.get(AVG_READING_TIME)));
        analytics.setAverageScrollDepth(safeConvertToDouble(stats.get("avg_scroll_depth")));
        analytics.setCompletionRate(safeConvertToDouble(stats.get("completion_rate")));
        analytics.setSkipRate(safeConvertToDouble(stats.get("skip_rate")));
        analytics.setRereadRate(safeConvertToDouble(stats.get("reread_rate")));
        analytics.setAverageRating(safeConvertToDouble(stats.get("avg_rating")));
        analytics.setTotalRatings(safeConvertToInt(stats.get(TOTAL_RATINGS)));
        analytics.setTotalBookmarks(safeConvertToInt(stats.get("total_bookmarks")));
        analytics.setTotalHighlights(safeConvertToInt(stats.get("total_highlights")));
        analytics.setTotalNotes(safeConvertToInt(stats.get("total_notes")));
        analytics.setTotalComments(safeConvertToInt(stats.get("total_comments")));

        analytics.setEngagementScore(calculateEngagementScore(analytics));
        analytics.setPopularityLevel(determinePopularityLevel(analytics));
        analytics.setDifficultyLevel(determineDifficultyLevel(analytics));

        analytics.setTopHighlights(findTopHighlightsForChapter(book.getId(), chapter.getChapterNumber()));

        return analytics;
    }

    // ============================================
    // HELPER METHODS - Mapping & Conversion
    // ============================================

    private ChapterEngagement mapToChapterEngagement(Map<String, Object> data) {
        ChapterEngagement engagement = new ChapterEngagement();
        engagement.setChapterNumber(safeConvertToInt(data.get(CHAPTER_NUMBER)));
        engagement.setChapterTitle((String) data.get(CHAPTER_TITLE));
        engagement.setAnnotationCount(safeConvertToInt(data.get("annotation_count")));
        engagement.setEngagementScore(safeConvertToDouble(data.get("engagement_score")));
        engagement.setUniqueReaders(safeConvertToInt(data.get("unique_readers")));
        return engagement;
    }

    private PopularPassage mapToPopularPassage(Map<String, Object> data) {
        PopularPassage passage = new PopularPassage();
        passage.setId(safeConvertToLong(data.get("id")));
        passage.setChapterNumber(safeConvertToInt(data.get(CHAPTER_NUMBER)));
        passage.setChapterTitle((String) data.get(CHAPTER_TITLE));
        passage.setPassage((String) data.get("passage"));
        passage.setStartPosition(safeConvertToInt(data.get("start_position")));
        passage.setEndPosition(safeConvertToInt(data.get("end_position")));
        passage.setHighlightCount(safeConvertToInt(data.get(HIGHLIGHT_COUNT)));
        passage.setPopularityScore(safeConvertToDouble(data.get("popularity_score")));

        Object colorsObj = data.get("common_colors");
        if (colorsObj != null) {
            passage.setCommonColors(parseArray(colorsObj));
        }

        return passage;
    }

    private ChapterDropOffPoint mapToDropOffPoint(Map<String, Object> data) {
        ChapterDropOffPoint point = new ChapterDropOffPoint();
        point.setChapterNumber(safeConvertToInt(data.get(CHAPTER_NUMBER)));
        point.setChapterTitle((String) data.get(CHAPTER_TITLE));
        point.setDropOffRate(safeConvertToDouble(data.get("drop_off_rate")));
        point.setAverageScrollDepth(safeConvertToInt(data.get("average_scroll_depth")));
        point.setReadersStarted(safeConvertToInt(data.get("readers_started")));
        point.setReadersCompleted(safeConvertToInt(data.get("readers_completed")));

        double dropOffRate = point.getDropOffRate();
        if (dropOffRate >= 50) point.setSeverity("Critical");
        else if (dropOffRate >= 35) point.setSeverity("High");
        else if (dropOffRate >= 20) point.setSeverity("Medium");
        else point.setSeverity("Low");

        return point;
    }

    private ExportAnnotationsResponse mapToExportResponse(AnnotationExport export) {
        ExportAnnotationsResponse response = new ExportAnnotationsResponse();
        response.setExportId(export.getId());
        response.setStatus(export.getStatus());
        response.setFileUrl(export.getFileUrl());
        response.setFileName(export.getFileName());
        response.setFileSize(export.getFileSize());
        response.setExportType(export.getExportType());
        response.setTotalBookmarks(export.getTotalBookmarks());
        response.setTotalHighlights(export.getTotalHighlights());
        response.setTotalNotes(export.getTotalNotes());
        response.setRequestedAt(export.getCreatedAt());
        response.setCompletedAt(export.getCompletedAt());
        response.setExpiresAt(export.getExpiresAt());
        response.setErrorMessage(export.getErrorMessage());
        return response;
    }

    private List<PopularHighlight> findTopHighlightsForChapter(Long bookId, Integer chapterNumber) {
        List<Map<String, Object>> data = analyticsMapper.getTopHighlightsForChapter(bookId, chapterNumber);

        return data.stream()
                .map(d -> {
                    PopularHighlight highlight = new PopularHighlight();
                    highlight.setText((String) d.get("text"));
                    highlight.setHighlightCount(safeConvertToInt(d.get(HIGHLIGHT_COUNT)));
                    highlight.setPosition(safeConvertToInt(d.get("position")));
                    return highlight;
                })
                .collect(Collectors.toList());
    }

    // ============================================
    // HELPER METHODS - Calculation & Logic
    // ============================================

    private int calculateEngagementScore(ChapterAnalyticsResponse analytics) {
        int score = 0;

        score += (int) Math.min(analytics.getCompletionRate() * 0.3, 30);

        int totalAnnotations = analytics.getTotalBookmarks() +
                analytics.getTotalHighlights() +
                analytics.getTotalNotes();
        score += (int) Math.min(totalAnnotations * 0.5, 25);

        if (analytics.getAverageRating() != null) {
            score += (int) ((analytics.getAverageRating() / 5.0) * 20);
        }

        score += (int) Math.min(analytics.getTotalComments() * 0.3, 15);

        if (analytics.getSkipRate() != null) {
            score += (int) Math.max(0, 10 - (analytics.getSkipRate() * 0.2));
        }

        return Math.min(score, 100);
    }

    private String determinePopularityLevel(ChapterAnalyticsResponse analytics) {
        int score = analytics.getEngagementScore();

        if (score >= 80) return "Very High";
        if (score >= 60) return "High";
        if (score >= 40) return "Medium";
        return "Low";
    }

    private String determineDifficultyLevel(ChapterAnalyticsResponse analytics) {
        double skipRate = analytics.getSkipRate() != null ? analytics.getSkipRate() : 0;
        double completionRate = analytics.getCompletionRate() != null ? analytics.getCompletionRate() : 100;
        int readingTime = analytics.getAverageReadingTimeSeconds() != null ?
                analytics.getAverageReadingTimeSeconds() : 0;

        int difficultyScore = 0;

        if (skipRate > 30) difficultyScore += 3;
        else if (skipRate > 15) difficultyScore += 2;
        else difficultyScore += 1;

        if (completionRate < 50) difficultyScore += 3;
        else if (completionRate < 75) difficultyScore += 2;
        else difficultyScore += 1;

        if (readingTime > 1800) difficultyScore += 3;
        else if (readingTime > 900) difficultyScore += 2;
        else difficultyScore += 1;

        if (difficultyScore >= 8) return "Very Difficult";
        if (difficultyScore >= 6) return "Difficult";
        if (difficultyScore >= 4) return MODERATE;
        return "Easy";
    }

    private String inferSkipReason(ChapterSkipAnalysis analysis) {
        if (analysis.getSkipRate() > 50) {
            return "Critical engagement issue - needs review";
        } else if (analysis.getSkipRate() > 30) {
            return "Possibly too long or complex";
        } else {
            return "Normal skip rate";
        }
    }

    private int calculateCurrentStreak(List<ReadingSession> sessions) {
        if (sessions.isEmpty()) return 0;

        List<LocalDateTime> dates = sessions.stream()
                .map(ReadingSession::getStartedAt)
                .map(dt -> dt.toLocalDate().atStartOfDay())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        int streak = 0;
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).toLocalDate().atStartOfDay();

        for (LocalDateTime date : dates) {
            if (date.isEqual(yesterday) || date.isAfter(yesterday)) {
                streak++;
                yesterday = date.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }

    private List<String> parseArray(Object arrayObj) {
        // Handle PostgreSQL array conversion
        switch (arrayObj) {
            case null -> {
                return new ArrayList<>();
            }
            case String[] strings -> {
                return Arrays.asList(strings);
            }
            case List<?> ignored -> {
                return (List<String>) arrayObj;
            }
            default -> {
            }
        }

        // Parse string representation: {val1,val2,val3}
        String arrayStr = arrayObj.toString();
        if (arrayStr.startsWith("{") && arrayStr.endsWith("}")) {
            arrayStr = arrayStr.substring(1, arrayStr.length() - 1);
            return Arrays.asList(arrayStr.split(","));
        }

        return new ArrayList<>();
    }
}