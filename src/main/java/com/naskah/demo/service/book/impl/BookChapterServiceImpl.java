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

    private final HeaderHolder headerHolder;
    private final EntityResponseMapper entityMapper;

    private static final String SUCCESS = "Success";

    // ============ CHAPTER READING ============

    @Override
    @Cacheable(value = "chapters", key = "#slug + ':' + #chapterNumber")
    public DataResponse<ChapterReadingResponse> readChapter(String slug, Integer chapterNumber) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);
            if (chapter == null) {
                throw new DataNotFoundException();
            }

            ChapterReadingResponse response = new ChapterReadingResponse();
            response.setBookId(book.getId());
            response.setBookTitle(book.getTitle());
            response.setChapterId(chapter.getId());
            response.setChapterNumber(chapter.getChapterNumber());
            response.setChapterTitle(chapter.getTitle());
            response.setHtmlContent(chapter.getHtmlContent());
            response.setWordCount(chapter.getWordCount());
            response.setEstimatedReadTime(calculateReadTime(chapter.getWordCount()));
            response.setTotalChapters(book.getTotalPages());

            // ✅ Set hierarchy info
            response.setParentChapterId(chapter.getParentChapterId());
            response.setChapterLevel(chapter.getChapterLevel());

            // ✅ Set navigation (previous/next/parent)
            setChapterNavigation(response, book.getId(), chapter);

            // Get user-specific data if authenticated
            String username = headerHolder.getUsername();
            if (username != null && !username.isEmpty()) {
                User user = userMapper.findUserByUsername(username);
                if (user != null) {
                    response.setBookmarks(getUserChapterBookmarks(user.getId(), book.getId(), chapterNumber));
                    response.setHighlights(getUserChapterHighlights(user.getId(), book.getId(), chapterNumber));
                    response.setNotes(getUserChapterNotes(user.getId(), book.getId(), chapterNumber));

                    ChapterProgress progress = chapterProgressMapper.findProgress(
                            user.getId(), book.getId(), chapterNumber
                    );
                    if (progress != null) {
                        response.setCurrentPosition(progress.getPosition());
                        response.setIsCompleted(progress.getIsCompleted());
                    }

                    updateReadingHeatmap(book.getId(), chapterNumber);
                }
            }

            log.info("User {} reading chapter {} of book {}", username, chapterNumber, slug);

            return new DataResponse<>(SUCCESS, "Chapter retrieved successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error reading chapter {} of book {}: {}", chapterNumber, slug, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ Set chapter navigation (prev/next/parent) with slug support
     */
    private void setChapterNavigation(ChapterReadingResponse response, Long bookId, BookChapter currentChapter) {
        // Previous chapter (by chapter number)
        BookChapter prevChapter = chapterMapper.findChapterByNumber(
                bookId, currentChapter.getChapterNumber() - 1
        );
        if (prevChapter != null) {
            ChapterNavigationInfo prev = new ChapterNavigationInfo();
            prev.setChapterNumber(prevChapter.getChapterNumber());
            prev.setTitle(prevChapter.getTitle());
            prev.setChapterLevel(prevChapter.getChapterLevel());
            prev.setSlug(prevChapter.getSlug());
            if (prevChapter.getParentChapterId() != null) {
                BookChapter parent = chapterMapper.findChapterById(prevChapter.getParentChapterId());
                if (parent != null) {
                    prev.setParentSlug(parent.getSlug());
                }
            }

            response.setPreviousChapter(prev);
        }

        // Next chapter (by chapter number)
        BookChapter nextChapter = chapterMapper.findChapterByNumber(
                bookId, currentChapter.getChapterNumber() + 1
        );
        if (nextChapter != null) {
            ChapterNavigationInfo next = new ChapterNavigationInfo();
            next.setChapterNumber(nextChapter.getChapterNumber());
            next.setTitle(nextChapter.getTitle());
            next.setChapterLevel(nextChapter.getChapterLevel());
            next.setSlug(nextChapter.getSlug());
            if (nextChapter.getParentChapterId() != null) {
                BookChapter parent = chapterMapper.findChapterById(nextChapter.getParentChapterId());
                if (parent != null) {
                    next.setParentSlug(parent.getSlug());
                }
            }

            response.setNextChapter(next);
        }

        // Parent chapter (if exists)
        if (currentChapter.getParentChapterId() != null) {
            BookChapter parentChapter = chapterMapper.findChapterById(
                    currentChapter.getParentChapterId()
            );
            if (parentChapter != null) {
                ChapterNavigationInfo parent = new ChapterNavigationInfo();
                parent.setChapterNumber(parentChapter.getChapterNumber());
                parent.setTitle(parentChapter.getTitle());
                parent.setChapterLevel(parentChapter.getChapterLevel());
                parent.setSlug(parentChapter.getSlug());  // ✅ Add slug
                response.setParentChapter(parent);
            }
        }
    }

    @Override
    @Cacheable(value = "chapter-list", key = "#slug")
    public DataResponse<List<ChapterSummaryResponse>> getAllChaptersSummary(String slug) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            List<BookChapter> chapters = chapterMapper.findChaptersByBookId(book.getId());

            String username = headerHolder.getUsername();
            Long userId = null;
            if (username != null) {
                User user = userMapper.findUserByUsername(username);
                if (user != null) {
                    userId = user.getId();
                }
            }

            // ✅ Build hierarchical structure
            List<ChapterSummaryResponse> hierarchicalChapters = buildChapterHierarchy(
                    chapters, book.getId(), userId
            );

            return new DataResponse<>(SUCCESS, "Chapters retrieved successfully",
                    HttpStatus.OK.value(), hierarchicalChapters);

        } catch (Exception e) {
            log.error("Error getting chapters for book {}: {}", slug, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ Build hierarchical chapter structure
     */
    private List<ChapterSummaryResponse> buildChapterHierarchy(
            List<BookChapter> chapters, Long bookId, Long userId) {

        // Convert to response objects
        Map<Long, ChapterSummaryResponse> chapterMap = new HashMap<>();
        List<ChapterSummaryResponse> rootChapters = new ArrayList<>();

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
            response.setSubChapters(new ArrayList<>());  // Initialize sub-chapters list

            // Check if user has completed this chapter
            if (userId != null) {
                ChapterProgress progress = chapterProgressMapper.findProgress(
                        userId, bookId, chapter.getChapterNumber()
                );
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
                // Root level chapter
                rootChapters.add(response);
            } else {
                // Sub-chapter - add to parent
                ChapterSummaryResponse parent = chapterMap.get(chapter.getParentChapterId());
                if (parent != null) {
                    parent.getSubChapters().add(response);
                } else {
                    // Parent not found, treat as root
                    rootChapters.add(response);
                }
            }
        }

        return rootChapters;
    }

    // ============ TEXT-TO-SPEECH SUPPORT ============

    /**
     * Get chapter text for browser TTS (Web Speech API)
     * Returns plain text extracted from HTML
     */
    @Override
    @Cacheable(value = "chapter-text", key = "#slug + ':' + #chapterNumber")
    public DataResponse<ChapterTextResponse> getChapterTextForTTS(String slug, Integer chapterNumber) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

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

            log.info("Chapter text for TTS retrieved: book {}, chapter {}", slug, chapterNumber);

            return new DataResponse<>(SUCCESS, "Chapter text retrieved successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting chapter text for TTS: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get chapter paragraphs for sentence-by-sentence TTS with highlighting
     */
    @Override
    @Cacheable(value = "chapter-paragraphs", key = "#slug + ':' + #chapterNumber")
    public DataResponse<ChapterParagraphsResponse> getChapterParagraphs(String slug, Integer chapterNumber) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

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

            return new DataResponse<>(SUCCESS, "Chapter paragraphs retrieved successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting chapter paragraphs: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============ CHAPTER PROGRESS ============

    @Override
    @Transactional
    public DataResponse<ChapterProgressResponse> saveChapterProgress(
            String slug, Integer chapterNumber, ChapterProgressRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

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

            updateOverallBookProgress(user.getId(), book.getId());

            ChapterProgressResponse response = new ChapterProgressResponse();
            response.setChapterNumber(chapterNumber);
            response.setPosition(progress.getPosition());
            response.setReadingTimeSeconds(progress.getReadingTimeSeconds());
            response.setIsCompleted(progress.getIsCompleted());
            response.setLastReadAt(progress.getLastReadAt());

            return new DataResponse<>(SUCCESS, "Progress saved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error saving chapter progress: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============ CHAPTER ANNOTATIONS ============

    @Override
    public DataResponse<ChapterAnnotationsResponse> getMyChapterAnnotations(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

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

    @Override
    @Transactional
    public DataResponse<BookmarkResponse> addChapterBookmark(String slug, Integer chapterNumber, ChapterBookmarkRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Bookmark bookmark = new Bookmark();
            bookmark.setUserId(user.getId());
            bookmark.setBookId(book.getId());
            bookmark.setChapterNumber(chapterNumber);
            bookmark.setPosition(String.valueOf(request.getPosition()));
            bookmark.setTitle(request.getTitle());
            bookmark.setDescription(request.getDescription());
            bookmark.setColor(request.getColor() != null ? request.getColor() : "#de96be");
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
    public DataResponse<HighlightResponse> addChapterHighlight(
            String slug, Integer chapterNumber, ChapterHighlightRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Highlight highlight = new Highlight();
            highlight.setUserId(user.getId());
            highlight.setBookId(book.getId());
            highlight.setChapterNumber(chapterNumber);
            highlight.setStartPosition(String.valueOf(request.getStartPosition()));
            highlight.setEndPosition(String.valueOf(request.getEndPosition()));
            highlight.setHighlightedText(request.getHighlightedText());
            highlight.setColor(request.getColor());
            highlight.setNote(request.getNote());
            highlight.setCreatedAt(LocalDateTime.now());
            highlight.setUpdatedAt(LocalDateTime.now());

            highlightMapper.insertHighlight(highlight);
            analyticsMapper.updateHighlightHeatmap(book.getId(), chapterNumber, 1);

            HighlightResponse response = entityMapper.toHighlightResponse(highlight);
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
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            Note note = new Note();
            note.setUserId(user.getId());
            note.setBookId(book.getId());
            note.setChapterNumber(chapterNumber);
            note.setPosition(String.valueOf(request.getPosition()));
            note.setTitle(request.getTitle());
            note.setContent(request.getContent());
            note.setColor(request.getColor() != null ? request.getColor() : "#FFEB3B");
            note.setIsPrivate(request.getIsPrivate());
            note.setCreatedAt(LocalDateTime.now());
            note.setUpdatedAt(LocalDateTime.now());

            noteMapper.insertNote(note);
            analyticsMapper.updateNoteHeatmap(book.getId(), chapterNumber, 1);

            NoteResponse response = entityMapper.toNoteResponse(note);
            return new DataResponse<>(SUCCESS, "Note added successfully", HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding chapter note: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============ CHAPTER REVIEWS ============

    @Override
    public DataResponse<List<ChapterReviewResponse>> getChapterReviews(String slug, Integer chapterNumber, int page, int limit) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            int offset = (page - 1) * limit;
            List<ChapterReview> reviews = chapterReviewMapper.findReviewsByChapter(book.getId(), chapterNumber, offset, limit);

            String username = headerHolder.getUsername();
            Long currentUserId = null;
            if (username != null) {
                User user = userMapper.findUserByUsername(username);
                if (user != null) {
                    currentUserId = user.getId();
                }
            }

            final Long finalUserId = currentUserId;
            List<ChapterReviewResponse> responses = reviews.stream()
                    .map(review -> mapToChapterReviewResponse(review, finalUserId))
                    .collect(Collectors.toList());

            return new DataResponse<>(SUCCESS, "Reviews retrieved successfully", HttpStatus.OK.value(), responses);

        } catch (Exception e) {
            log.error("Error getting chapter reviews: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<ChapterReviewResponse> addChapterReview(
            String slug, Integer chapterNumber, ChapterReviewRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            ChapterReview review = new ChapterReview();
            review.setUserId(user.getId());
            review.setBookId(book.getId());
            review.setChapterNumber(chapterNumber);
            review.setComment(request.getComment());
            review.setIsSpoiler(request.getIsSpoiler());
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());

            chapterReviewMapper.insertChapterReview(review);

            ChapterReviewResponse response = mapToChapterReviewResponse(review, user.getId());
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
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);
            ChapterReview parentReview = chapterReviewMapper.findById(reviewId);

            if (user == null || book == null || parentReview == null) {
                throw new DataNotFoundException();
            }

            ChapterReview reply = new ChapterReview();
            reply.setUserId(user.getId());
            reply.setBookId(book.getId());
            reply.setChapterNumber(chapterNumber);
            reply.setComment(request.getComment());
            reply.setParentId(reviewId);
            reply.setCreatedAt(LocalDateTime.now());
            reply.setUpdatedAt(LocalDateTime.now());

            chapterReviewMapper.insertChapterReview(reply);

            ChapterReviewResponse response = mapToChapterReviewResponse(reply, user.getId());
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
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            if (user == null) {
                throw new DataNotFoundException();
            }

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
    public DataResponse<Void> unlikeChapterReview(String slug, Integer chapterNumber, Long reviewId) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            if (user == null) {
                throw new DataNotFoundException();
            }

            chapterReviewMapper.unlikeReview(reviewId, user.getId());
            chapterReviewMapper.decrementLikeCount(reviewId);

            return new DataResponse<>(SUCCESS, "Review unliked successfully", HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error unliking chapter review: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ============ CHAPTER STATISTICS ============

    @Override
    @Cacheable(value = "chapter-stats", key = "#slug + ':' + #chapterNumber")
    public DataResponse<ChapterStatsResponse> getChapterStats(String slug, Integer chapterNumber) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);
            if (chapter == null) {
                throw new DataNotFoundException();
            }

            ChapterStatsResponse response = new ChapterStatsResponse();
            response.setChapterNumber(chapterNumber);
            response.setChapterTitle(chapter.getTitle());

            Map<String, Object> stats = analyticsMapper.getChapterStats(book.getId(), chapterNumber);

            // Safe casting - database returns Long, convert to Integer
            response.setReaderCount(safeConvertToInt(stats.get("reader_count")));
            response.setCompletionRate(safeConvertToDouble(stats.get("completion_rate")));
            response.setAverageReadingTimeMinutes(safeConvertToInt(stats.get("avg_reading_time")));
            response.setHighlightCount(safeConvertToInt(stats.get("highlight_count")));
            response.setNoteCount(safeConvertToInt(stats.get("note_count")));
            response.setCommentCount(safeConvertToInt(stats.get("comment_count")));

            return new DataResponse<>(SUCCESS, "Statistics retrieved successfully", HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting chapter stats: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Safe conversion helper methods
    private Integer safeConvertToInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private Double safeConvertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteChapterBookmark(String slug, Integer chapterNumber, Long bookmarkId) {
        String username = headerHolder.getUsername();
        if (username == null || username.isEmpty()) {
            throw new UnauthorizedException();
        }

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

        // Verify bookmark exists and belongs to user
        Bookmark bookmark = bookmarkMapper.findBookmarkById(bookmarkId);
        if (bookmark == null) {
            throw new DataNotFoundException();
        }

        if (!bookmark.getUserId().equals(user.getId())) {
            throw new UnauthorizedException();
        }

        bookmarkMapper.deleteBookmark(bookmarkId);

        log.info("User {} deleted bookmark {} from book {}", user.getId(), bookmarkId, slug);

        return new DataResponse<>(SUCCESS, "Penanda buku berhasil dihapus", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteChapterHighlight(String slug, Integer chapterNumber, Long highlightId) {
        String username = headerHolder.getUsername();
        if (username == null || username.isEmpty()) {
            throw new UnauthorizedException();
        }

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

        // Verify highlight exists and belongs to user
        Highlight highlight = highlightMapper.findHighlightById(highlightId);
        if (highlight == null) {
            throw new DataNotFoundException();
        }

        if (!highlight.getUserId().equals(user.getId())) {
            throw new UnauthorizedException();
        }

        highlightMapper.deleteHighlight(highlightId);

        // Update analytics
        analyticsMapper.updateHighlightHeatmap(book.getId(), chapterNumber, -1);

        log.info("User {} deleted highlight {} from book {}", user.getId(), highlightId, slug);

        return new DataResponse<>(SUCCESS, "Highlight berhasil dihapus", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteChapterNote(String slug, Integer chapterNumber, Long noteId) {
        String username = headerHolder.getUsername();
        if (username == null || username.isEmpty()) {
            throw new UnauthorizedException();
        }

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

        // Verify note exists and belongs to user
        Note note = noteMapper.findNoteById(noteId);
        if (note == null) {
            throw new DataNotFoundException();
        }

        if (!note.getUserId().equals(user.getId())) {
            throw new UnauthorizedException();
        }

        noteMapper.deleteNote(noteId);

        // Update analytics
        analyticsMapper.updateNoteHeatmap(book.getId(), chapterNumber, -1);

        log.info("User {} deleted note {} from book {}", user.getId(), noteId, slug);

        return new DataResponse<>(SUCCESS, "Catatan berhasil dihapus", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<ChapterReadingResponse> readChapterBySlugPath(String bookSlug, String slugPath) {
        try {
            Book book = bookMapper.findBookBySlug(bookSlug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            // Split slug path: kerikil-tajam/nisan -> ["kerikil-tajam", "nisan"]
            String[] slugParts = slugPath.split("/");

            // Find chapter by navigating through hierarchy
            BookChapter chapter = findChapterBySlugHierarchy(book.getId(), slugParts);

            if (chapter == null) {
                throw new DataNotFoundException();
            }

            // Build response (similar to existing readChapter method)
            ChapterReadingResponse response = buildChapterReadingResponse(book, chapter);

            return new DataResponse<>(SUCCESS, "Chapter retrieved successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error reading chapter by path {}: {}", slugPath, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Find chapter by navigating slug hierarchy
     */
    private BookChapter findChapterBySlugHierarchy(Long bookId, String[] slugParts) {
        Long currentParentId = null;
        BookChapter currentChapter = null;

        for (String slug : slugParts) {
            currentChapter = chapterMapper.findChapterBySlugAndParent(
                    bookId, slug, currentParentId
            );

            if (currentChapter == null) {
                return null;
            }

            currentParentId = currentChapter.getId();
        }

        return currentChapter;
    }

    /**
     * Build chapter reading response with all details
     */
    private ChapterReadingResponse buildChapterReadingResponse(Book book, BookChapter chapter) {
        ChapterReadingResponse response = new ChapterReadingResponse();
        response.setBookId(book.getId());
        response.setBookTitle(book.getTitle());
        response.setChapterId(chapter.getId());
        response.setChapterNumber(chapter.getChapterNumber());
        response.setChapterTitle(chapter.getTitle());
        response.setSlug(chapter.getSlug());
        response.setContent(chapter.getContent());
        response.setHtmlContent(chapter.getHtmlContent());
        response.setWordCount(chapter.getWordCount());
        response.setEstimatedReadTime(calculateReadTime(chapter.getWordCount()));
        response.setTotalChapters(book.getTotalPages());

        // Set hierarchy info
        response.setParentChapterId(chapter.getParentChapterId());
        response.setChapterLevel(chapter.getChapterLevel());

        // ✅ Build breadcrumbs
        response.setBreadcrumbs(buildBreadcrumbs(chapter));

        // Set navigation
        setChapterNavigation(response, book.getId(), chapter);

        // Get user-specific data if authenticated
        String username = headerHolder.getUsername();
        if (username != null && !username.isEmpty()) {
            User user = userMapper.findUserByUsername(username);
            if (user != null) {
                response.setBookmarks(getUserChapterBookmarks(user.getId(), book.getId(), chapter.getChapterNumber()));
                response.setHighlights(getUserChapterHighlights(user.getId(), book.getId(), chapter.getChapterNumber()));
                response.setNotes(getUserChapterNotes(user.getId(), book.getId(), chapter.getChapterNumber()));

                ChapterProgress progress = chapterProgressMapper.findProgress(
                        user.getId(), book.getId(), chapter.getChapterNumber()
                );
                if (progress != null) {
                    response.setCurrentPosition(progress.getPosition());
                    response.setIsCompleted(progress.getIsCompleted());
                }

                updateReadingHeatmap(book.getId(), chapter.getChapterNumber());
            }
        }

        log.info("User {} reading chapter {} (slug: {}) of book {}",
                username, chapter.getChapterNumber(), chapter.getSlug(), book.getSlug());

        return response;
    }

    /**
     * Build breadcrumbs for chapter navigation
     */
    private List<ChapterBreadcrumb> buildBreadcrumbs(BookChapter chapter) {
        List<ChapterBreadcrumb> breadcrumbs = new ArrayList<>();

        // Start from current chapter and traverse up to root
        BookChapter current = chapter;

        while (current != null) {
            ChapterBreadcrumb breadcrumb = new ChapterBreadcrumb();
            breadcrumb.setChapterId(current.getId());
            breadcrumb.setTitle(current.getTitle());
            breadcrumb.setSlug(current.getSlug());
            breadcrumb.setChapterLevel(current.getChapterLevel());

            // Add at the beginning to maintain order (root -> ... -> current)
            breadcrumbs.addFirst(breadcrumb);

            // Move to parent
            if (current.getParentChapterId() != null) {
                current = chapterMapper.findChapterById(current.getParentChapterId());
            } else {
                current = null;
            }
        }

        return breadcrumbs;
    }

    // ============ HELPER METHODS ============

    // Service
    private List<BookmarkResponse> getUserChapterBookmarks(Long userId, Long bookId, Integer chapterNumber) {
        List<Bookmark> bookmarks = bookmarkMapper.findByUserBookAndPage(userId, bookId, chapterNumber);

        // ✅ Get all unique chapter numbers
        Set<Integer> chapterNumbers = bookmarks.stream()
                .map(Bookmark::getChapterNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // ✅ Batch fetch all chapter info in one query
        Map<Integer, ChapterNavigationInfo> chapterInfoMap = getChapterInfoMap(bookId, chapterNumbers);

        // ✅ Map to response with chapter info
        return bookmarks.stream()
                .map(bookmark -> {
                    BookmarkResponse response = entityMapper.toBookmarkResponse(bookmark);
                    ChapterNavigationInfo chapterInfo = chapterInfoMap.get(bookmark.getChapterNumber());
                    response.setChapter(chapterInfo);
                    return response;
                })
                .collect(Collectors.toList());
    }

    private List<HighlightResponse> getUserChapterHighlights(Long userId, Long bookId, Integer chapterNumber) {
        List<Highlight> highlights = highlightMapper.findByUserBookAndPage(userId, bookId, chapterNumber);

        Set<Integer> chapterNumbers = highlights.stream()
                .map(Highlight::getChapterNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, ChapterNavigationInfo> chapterInfoMap = getChapterInfoMap(bookId, chapterNumbers);

        return highlights.stream()
                .map(highlight -> {
                    HighlightResponse response = entityMapper.toHighlightResponse(highlight);
                    ChapterNavigationInfo chapterInfo = chapterInfoMap.get(highlight.getChapterNumber());
                    response.setChapter(chapterInfo);
                    return response;
                })
                .collect(Collectors.toList());
    }

    private List<NoteResponse> getUserChapterNotes(Long userId, Long bookId, Integer chapterNumber) {
        List<Note> notes = noteMapper.findByUserBookAndPage(userId, bookId, chapterNumber);

        Set<Integer> chapterNumbers = notes.stream()
                .map(Note::getChapterNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, ChapterNavigationInfo> chapterInfoMap = getChapterInfoMap(bookId, chapterNumbers);

        return notes.stream()
                .map(note -> {
                    NoteResponse response = entityMapper.toNoteResponse(note);
                    ChapterNavigationInfo chapterInfo = chapterInfoMap.get(note.getChapterNumber());
                    response.setChapter(chapterInfo);
                    return response;
                })
                .collect(Collectors.toList());
    }

    // ✅ Helper: Batch fetch chapter info
    private Map<Integer, ChapterNavigationInfo> getChapterInfoMap(Long bookId, Set<Integer> chapterNumbers) {
        if (chapterNumbers.isEmpty()) return Collections.emptyMap();

        List<BookChapter> chapters = chapterMapper.findChaptersByBookAndNumbers(bookId, chapterNumbers);

        // Get all parent IDs
        Set<Long> parentIds = chapters.stream()
                .map(BookChapter::getParentChapterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Batch fetch parents
        Map<Long, String> parentSlugMap = new HashMap<>();
        if (!parentIds.isEmpty()) {
            List<BookChapter> parents = chapterMapper.findChaptersByIds(parentIds);
            parentSlugMap = parents.stream()
                    .collect(Collectors.toMap(BookChapter::getId, BookChapter::getSlug));
        }

        // Build map
        Map<Long, String> finalParentSlugMap = parentSlugMap;
        return chapters.stream()
                .collect(Collectors.toMap(
                        BookChapter::getChapterNumber,
                        chapter -> {
                            ChapterNavigationInfo info = new ChapterNavigationInfo();
                            info.setChapterNumber(chapter.getChapterNumber());
                            info.setTitle(chapter.getTitle());
                            info.setSlug(chapter.getSlug());
                            info.setChapterLevel(chapter.getChapterLevel());

                            if (chapter.getParentChapterId() != null) {
                                info.setParentSlug(finalParentSlugMap.get(chapter.getParentChapterId()));
                            }

                            return info;
                        }
                ));
    }

    private ChapterReviewResponse mapToChapterReviewResponse(ChapterReview review, Long currentUserId) {
        User reviewUser = userMapper.findUserById(review.getUserId());

        ChapterReviewResponse response = new ChapterReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setUserName(reviewUser != null ? reviewUser.getUsername() : "Unknown");
        response.setUserProfilePicture(reviewUser != null ? reviewUser.getProfilePictureUrl() : null);
        response.setChapterNumber(review.getChapterNumber());
        response.setComment(review.getComment());
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

    private String extractTextFromHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    private void updateReadingHeatmap(Long bookId, Integer chapterNumber) {
        try {
            analyticsMapper.updateReadingHeatmap(bookId, chapterNumber, 1);
        } catch (Exception e) {
            log.warn("Failed to update reading heatmap: {}", e.getMessage());
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

    private int calculateReadTime(int wordCount) {
        return Math.max(1, wordCount / 200);
    }

    // ============================================
    // 1. CHAPTER RATING
    // ============================================

    @Transactional
    public DataResponse<ChapterRatingResponse> rateChapter(
            String slug, Integer chapterNumber, ChapterRatingRequest request) {

        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);
        BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);

        if (user == null || book == null || chapter == null) {
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

        log.info("User {} rated chapter {} of book {} with {} stars",
                user.getId(), chapterNumber, slug, request.getRating());

        return new DataResponse<>(SUCCESS, "Rating submitted successfully",
                HttpStatus.OK.value(), response);
    }

    public DataResponse<ChapterRatingSummaryResponse> getChapterRatingSummary(
            String slug, Integer chapterNumber) {

        Book book = bookMapper.findBookBySlug(slug);
        BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), chapterNumber);

        if (book == null || chapter == null) {
            throw new DataNotFoundException();
        }

        Map<String, Object> summary = ratingMapper.getRatingSummary(book.getId(), chapterNumber);
        List<Map<String, Object>> distribution = ratingMapper.getRatingDistribution(book.getId(), chapterNumber);

        ChapterRatingSummaryResponse response = new ChapterRatingSummaryResponse();
        response.setChapterNumber(chapterNumber);
        response.setChapterTitle(chapter.getTitle());
        response.setAverageRating(getDoubleValue(summary.get("average_rating")));
        response.setTotalRatings(getIntValue(summary.get("total_ratings")));

        // Build distribution
        RatingDistribution dist = new RatingDistribution();
        for (Map<String, Object> d : distribution) {
            int rating = getIntValue(d.get("rating"));
            int count = getIntValue(d.get("count"));

            switch (rating) {
                case 1: dist.setOneStar(count); break;
                case 2: dist.setTwoStars(count); break;
                case 3: dist.setThreeStars(count); break;
                case 4: dist.setFourStars(count); break;
                case 5: dist.setFiveStars(count); break;
            }
        }
        response.setDistribution(dist);

        // Get user's rating if authenticated
        String username = headerHolder.getUsername();
        if (username != null) {
            User user = userMapper.findUserByUsername(username);
            if (user != null) {
                ChapterRating userRating = ratingMapper.findRating(
                        user.getId(), book.getId(), chapterNumber
                );
                response.setMyRating(userRating != null ? userRating.getRating() : null);
            }
        }

        return new DataResponse<>(SUCCESS, "Rating summary retrieved",
                HttpStatus.OK.value(), response);
    }

    @Transactional
    public DataResponse<Void> deleteChapterRating(String slug, Integer chapterNumber) {
        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

        ratingMapper.deleteRating(user.getId(), book.getId(), chapterNumber);

        return new DataResponse<>(SUCCESS, "Rating deleted", HttpStatus.OK.value(), null);
    }

    // ============================================
    // 2. READING ACTIVITY TRACKING
    // ============================================

    @Transactional
    public DataResponse<Void> startReading(String slug, StartReadingRequest request) {
        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

        // Create session if new
        ReadingSession session = sessionMapper.findBySessionId(request.getSessionId());
        if (session == null) {
            session = new ReadingSession();
            session.setUserId(user.getId());
            session.setBookId(book.getId());
            session.setSessionId(request.getSessionId());
            session.setStartedAt(LocalDateTime.now());
            session.setStartChapter(request.getChapterNumber());
            session.setDeviceType(request.getDeviceType());
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.insertSession(session);
        }

        // Create activity log
        ReadingActivityLog activity = new ReadingActivityLog();
        activity.setUserId(user.getId());
        activity.setBookId(book.getId());
        activity.setChapterNumber(request.getChapterNumber());
        activity.setSessionId(request.getSessionId());
        activity.setStartedAt(LocalDateTime.now());
        activity.setStartPosition(request.getStartPosition());
        activity.setDeviceType(request.getDeviceType());
        activity.setSource(request.getSource());
        activity.setIsSkip(false);
        activity.setIsReread(false);
        activity.setCreatedAt(LocalDateTime.now());

        activityMapper.insertActivity(activity);

        log.info("User {} started reading chapter {} of book {}",
                user.getId(), request.getChapterNumber(), slug);

        return new DataResponse<>(SUCCESS, "Reading started", HttpStatus.OK.value(), null);
    }

    @Transactional
    public DataResponse<Void> endReading(String slug, EndReadingRequest request) {
        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);
        BookChapter chapter = chapterMapper.findChapterByNumber(book.getId(), request.getChapterNumber());

        if (user == null || book == null || chapter == null) {
            throw new DataNotFoundException();
        }

        // Update activity log
        ReadingActivityLog activity = activityMapper.findBySessionAndChapter(
                request.getSessionId(), request.getChapterNumber()
        );

        if (activity != null) {
            LocalDateTime now = LocalDateTime.now();
            activity.setEndedAt(now);
            activity.setDurationSeconds(
                    (int) ChronoUnit.SECONDS.between(activity.getStartedAt(), now)
            );
            activity.setEndPosition(request.getEndPosition());
            activity.setScrollDepthPercentage(request.getScrollDepthPercentage());
            activity.setWordsRead(request.getWordsRead());
            activity.setInteractionCount(request.getInteractionCount());

            // Calculate reading speed
            if (request.getWordsRead() != null && activity.getDurationSeconds() > 0) {
                int wpm = (int) ((request.getWordsRead().doubleValue() / activity.getDurationSeconds()) * 60);
                activity.setReadingSpeedWpm(wpm);
            }

            // Detect skip (low scroll depth)
            if (request.getScrollDepthPercentage() != null && request.getScrollDepthPercentage() < 30) {
                activity.setIsSkip(true);
            }

            activityMapper.updateActivity(activity);
        }

        // Update session
        updateReadingSession(request.getSessionId(), book.getId(), request.getChapterNumber());

        // Async: Calculate patterns (would be better in async job)
        calculateUserReadingPattern(user.getId(), book.getId());

        log.info("User {} ended reading chapter {} of book {}",
                user.getId(), request.getChapterNumber(), slug);

        return new DataResponse<>(SUCCESS, "Reading ended", HttpStatus.OK.value(), null);
    }

    public DataResponse<ReadingHistoryResponse> getReadingHistory(String slug) {
        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

        // Get activity summary per chapter
        List<Map<String, Object>> summaryData = activityMapper.getUserChapterActivitySummary(
                user.getId(), book.getId()
        );

        List<ReadingActivitySummary> activities = summaryData.stream()
                .map(data -> {
                    ReadingActivitySummary summary = new ReadingActivitySummary();
                    summary.setChapterNumber(getIntValue(data.get("chapter_number")));

                    BookChapter chapter = chapterMapper.findChapterByNumber(
                            book.getId(), summary.getChapterNumber()
                    );
                    summary.setChapterTitle(chapter != null ? chapter.getTitle() : "");

                    summary.setTimesRead(getIntValue(data.get("times_read")));
                    summary.setTotalReadingTimeSeconds(getIntValue(data.get("total_duration")));
                    summary.setAverageScrollDepth(getDoubleValue(data.get("avg_scroll_depth")));
                    summary.setLastReadAt((LocalDateTime) data.get("last_read_at"));

                    // Check if completed
                    summary.setIsCompleted(
                            summary.getAverageScrollDepth() != null &&
                                    summary.getAverageScrollDepth() > 80
                    );

                    return summary;
                })
                .collect(Collectors.toList());

        // Get overall statistics
        Map<String, Object> stats = activityMapper.getUserBookStatistics(user.getId(), book.getId());

        ReadingStatistics statistics = new ReadingStatistics();
        statistics.setTotalChaptersRead(getIntValue(stats.get("chapters_read")));
        statistics.setTotalReadingTimeMinutes(getIntValue(stats.get("total_time")) / 60);
        statistics.setAverageReadingSpeedWpm(getIntValue(stats.get("avg_speed")));

        // Calculate completion rate
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

        return new DataResponse<>(SUCCESS, "Reading history retrieved",
                HttpStatus.OK.value(), response);
    }

    public DataResponse<UserReadingPatternResponse> getUserReadingPattern(String slug) {
        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

        UserReadingPattern pattern = patternMapper.findPattern(user.getId(), book.getId());

        if (pattern == null) {
            // Calculate if not exists
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

        return new DataResponse<>(SUCCESS, "Reading pattern retrieved",
                HttpStatus.OK.value(), response);
    }

    // ============================================
    // 3. SEARCH IN BOOK
    // ============================================

    public DataResponse<SearchInBookResponse> searchInBook(
            String slug, SearchInBookRequest request) {

        Book book = bookMapper.findBookBySlug(slug);
        if (book == null) throw new DataNotFoundException();

        int offset = (request.getPage() - 1) * request.getLimit();

        // Prepare query for full-text search
        String tsQuery = prepareSearchQuery(request.getQuery());

        List<Map<String, Object>> results = searchMapper.searchInBook(
                book.getId(), tsQuery, offset, request.getLimit()
        );

        int totalResults = searchMapper.countSearchResults(book.getId(), tsQuery);

        // Convert to response
        List<ChapterSearchResultResponse> searchResults = results.stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());

        SearchInBookResponse response = new SearchInBookResponse();
        response.setQuery(request.getQuery());
        response.setTotalResults(totalResults);
        response.setTotalChapters((int) results.stream()
                .map(r -> r.get("chapter_number"))
                .distinct()
                .count());
        response.setResults(searchResults);

        // Save search history
        String username = headerHolder.getUsername();
        if (username != null) {
            User user = userMapper.findUserByUsername(username);
            if (user != null) {
                SearchHistory history = new SearchHistory();
                history.setUserId(user.getId());
                history.setBookId(book.getId());
                history.setQuery(request.getQuery());
                history.setResultsCount(totalResults);
                history.setSearchType("in_book");
                history.setCreatedAt(LocalDateTime.now());
                searchMapper.insertSearchHistory(history);
            }
        }

        return new DataResponse<>(SUCCESS, "Search completed",
                HttpStatus.OK.value(), response);
    }

    // ============================================
    // 4. EXPORT ANNOTATIONS
    // ============================================

    @Transactional
    public DataResponse<ExportAnnotationsResponse> exportAnnotations(
            String slug, ExportAnnotationsRequest request) {

        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

        // Create export job
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
        export.setExpiresAt(LocalDateTime.now().plusDays(7)); // Expire in 7 days

        exportMapper.insertExport(export);

        // TODO: Trigger async job to process export
        // For now, process synchronously
        processExport(export, user, book);

        ExportAnnotationsResponse response = mapToExportResponse(export);

        return new DataResponse<>(SUCCESS, "Export started",
                HttpStatus.OK.value(), response);
    }

    public DataResponse<ExportHistoryResponse> getExportHistory(int page, int limit) {
        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new DataNotFoundException();

        int offset = (page - 1) * limit;
        List<AnnotationExport> exports = exportMapper.findUserExports(user.getId(), offset, limit);

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
                        .sum()
        );

        return new DataResponse<>(SUCCESS, "Export history retrieved",
                HttpStatus.OK.value(), response);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private void updateReadingSession(String sessionId, Long bookId, Integer endChapter) {
        ReadingSession session = sessionMapper.findBySessionId(sessionId);
        if (session != null) {
            session.setEndedAt(LocalDateTime.now());
            session.setEndChapter(endChapter);
            session.setTotalDurationSeconds(
                    (int) ChronoUnit.SECONDS.between(session.getStartedAt(), session.getEndedAt())
            );
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateSession(session);
        }
    }

    private void calculateUserReadingPattern(Long userId, Long bookId) {
        // This would ideally run as an async job
        // Implementation would analyze reading_activity_log and calculate patterns
        // For now, just a placeholder
        log.info("Calculating reading pattern for user {} and book {}", userId, bookId);
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

    private ChapterSearchResultResponse mapToSearchResult(Map<String, Object> data) {
        ChapterSearchResultResponse result = new ChapterSearchResultResponse();
        result.setChapterId(getLongValue(data.get("chapter_id")));
        result.setChapterNumber(getIntValue(data.get("chapter_number")));
        result.setChapterTitle((String) data.get("chapter_title"));
        result.setChapterSlug((String) data.get("chapter_slug"));
        result.setRelevanceScore(getFloatValue(data.get("relevance_score")));

        // Parse snippet into matches
        String snippet = (String) data.get("snippet");
        if (snippet != null) {
            SearchMatch match = new SearchMatch();
            match.setSnippet(snippet);
            result.setMatches(Collections.singletonList(match));
            result.setMatchCount(1);
        }

        return result;
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

    private String prepareSearchQuery(String query) {
        // Convert user query to tsquery format
        // Replace spaces with & for AND operation
        return String.join(" & ", query.trim().split("\\s+"));
    }

    private String getReadingTimeLabel(Integer hour) {
        if (hour == null) return "Unknown";
        if (hour >= 5 && hour < 12) return "Morning";
        if (hour >= 12 && hour < 17) return "Afternoon";
        if (hour >= 17 && hour < 21) return "Evening";
        return "Night";
    }

    private String getDayLabel(Integer day) {
        if (day == null) return "Unknown";
        String[] days = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        return day >= 1 && day <= 7 ? days[day] : "Unknown";
    }

    private String getReadingPaceLabel(Double chaptersPerDay) {
        if (chaptersPerDay == null) return "Unknown";
        if (chaptersPerDay < 1) return "Slow";
        if (chaptersPerDay < 3) return "Moderate";
        return "Fast";
    }

    private String getAnnotationStyleLabel(Double frequency) {
        if (frequency == null) return "Unknown";
        if (frequency < 1) return "Light";
        if (frequency < 3) return "Moderate";
        return "Heavy";
    }

    // Type conversion helpers
    private Integer getIntValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    private Long getLongValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private Double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    private Float getFloatValue(Object value) {
        if (value == null) return 0.0f;
        if (value instanceof Float) return (Float) value;
        if (value instanceof Double) return ((Double) value).floatValue();
        if (value instanceof Number) return ((Number) value).floatValue();
        return 0.0f;
    }

    // ============================================
    // 1. GET MY BOOK DATA (Bulk Endpoint)
    // ============================================

    @Cacheable(value = "user-book-data", key = "#slug + ':' + #username")
    public DataResponse<UserBookDataResponse> getMyBookData(String slug) {
        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

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
        response.setStatistics(calculateUserBookStatistics(user.getId(), book.getId(), response));

        log.info("Retrieved complete book data for user {} and book {}", user.getId(), slug);

        return new DataResponse<>(SUCCESS, "User book data retrieved",
                HttpStatus.OK.value(), response);
    }

    // ============================================
    // 2. GET BOOK ANALYTICS (for Authors/Admins)
    // ============================================

    @Cacheable(value = "book-analytics", key = "#slug + ':' + #dateFrom + ':' + #dateTo")
    public DataResponse<BookAnalyticsResponse> getBookAnalytics(
            String slug, String dateFrom, String dateTo) {

        // Check permission (should be author or admin)
        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        User user = userMapper.findUserByUsername(username);
        Book book = bookMapper.findBookBySlug(slug);

        if (user == null || book == null) {
            throw new DataNotFoundException();
        }

        // TODO: Check if user is author or admin of this book
        // if (!isAuthorOrAdmin(user, book)) throw new ForbiddenException();

        LocalDateTime startDate = parseDate(dateFrom);
        LocalDateTime endDate = parseDate(dateTo);

        BookAnalyticsResponse response = new BookAnalyticsResponse();
        response.setBookId(book.getId());
        response.setBookTitle(book.getTitle());
        response.setAnalyzedAt(LocalDateTime.now());
        response.setDateRange(formatDateRange(startDate, endDate));

        // 1. Overview
        response.setOverview(buildAnalyticsOverview(book.getId(), startDate, endDate));

        // 2. Reader Behavior
        response.setReaderBehavior(buildReaderBehaviorAnalytics(book.getId(), startDate, endDate));

        // 3. Content Engagement
        response.setContentEngagement(buildContentEngagementAnalytics(book.getId()));

        // 4. Popular Content
        response.setMostHighlightedPassages(findMostHighlightedPassages(book.getId(), 10));
        response.setMostCommonNotes(findMostCommonNotes(book.getId(), 5));

        // 5. Problem Areas
        response.setDropOffPoints(findDropOffPoints(book.getId()));
        response.setMostSkippedChapters(findMostSkippedChapters(book.getId()));

        // 6. Trends
        response.setTrends(analyzeTrends(book.getId(), startDate, endDate));

        log.info("Generated analytics for book {} by user {}", slug, user.getId());

        return new DataResponse<>(SUCCESS, "Analytics retrieved",
                HttpStatus.OK.value(), response);
    }

    // ============================================
    // 3. GET CHAPTERS ANALYTICS
    // ============================================

    @Cacheable(value = "chapters-analytics", key = "#slug")
    public DataResponse<List<ChapterAnalyticsResponse>> getChaptersAnalytics(String slug) {
        // Check permission
        String username = headerHolder.getUsername();
        if (username == null) throw new UnauthorizedException();

        Book book = bookMapper.findBookBySlug(slug);
        if (book == null) throw new DataNotFoundException();

        List<BookChapter> chapters = chapterMapper.findChaptersByBookId(book.getId());

        List<ChapterAnalyticsResponse> analytics = chapters.stream()
                .map(chapter -> buildChapterAnalytics(book, chapter))
                .collect(Collectors.toList());

        return new DataResponse<>(SUCCESS, "Chapter analytics retrieved",
                HttpStatus.OK.value(), analytics);
    }

    // ============================================
    // HELPER METHODS - User Book Data
    // ============================================

    private ReadingProgressSummary buildReadingProgressSummary(Long userId, Book book) {
        ReadingProgressSummary summary = new ReadingProgressSummary();

        List<ChapterProgress> allProgress = chapterProgressMapper.findAllByUserAndBook(userId, book.getId());

        // Find current chapter (last read incomplete chapter)
        ChapterProgress lastIncomplete = allProgress.stream()
                .filter(p -> !p.getIsCompleted())
                .max(Comparator.comparing(ChapterProgress::getLastReadAt))
                .orElse(null);

        if (lastIncomplete != null) {
            summary.setCurrentChapter(lastIncomplete.getChapterNumber());
        }

        summary.setTotalChapters(book.getTotalPages());

        // Completion percentage
        long completedCount = allProgress.stream()
                .filter(ChapterProgress::getIsCompleted)
                .count();
        double completion = (completedCount * 100.0) / book.getTotalPages();
        summary.setCompletionPercentage(completion);

        // Last read
        summary.setLastReadAt(allProgress.stream()
                .map(ChapterProgress::getLastReadAt)
                .max(Comparator.naturalOrder())
                .orElse(null));

        // Total time
        int totalTime = allProgress.stream()
                .mapToInt(ChapterProgress::getReadingTimeSeconds)
                .sum() / 60;
        summary.setTotalReadingTimeMinutes(totalTime);

        // Completed chapters
        summary.setCompletedChapters(allProgress.stream()
                .filter(ChapterProgress::getIsCompleted)
                .map(ChapterProgress::getChapterNumber)
                .collect(Collectors.toList()));

        // Chapters in progress
        summary.setChaptersInProgress((int) allProgress.stream()
                .filter(p -> !p.getIsCompleted() && p.getReadingTimeSeconds() > 0)
                .count());

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

        // Calculate streaks
        summary.setCurrentStreak(calculateCurrentStreak(sessions));
        summary.setLongestStreak(calculateLongestStreak(sessions));

        return summary;
    }

    private UserBookStatistics calculateUserBookStatistics(Long userId, Long bookId, UserBookDataResponse data) {
        UserBookStatistics stats = new UserBookStatistics();

        stats.setTotalBookmarks(data.getBookmarks().size());
        stats.setTotalHighlights(data.getHighlights().size());
        stats.setTotalNotes(data.getNotes().size());
        stats.setTotalRatings(data.getRatings().size());
        stats.setTotalSearches(data.getRecentSearches().size());
        stats.setTotalReadingSessions(data.getReadingHistory().getTotalSessions());

        // Calculate engagement score (0-100)
        double score = 0;
        score += Math.min(stats.getTotalBookmarks() * 2, 20); // Max 20 points
        score += Math.min(stats.getTotalHighlights() * 1.5, 20); // Max 20 points
        score += Math.min(stats.getTotalNotes() * 3, 20); // Max 20 points
        score += Math.min(stats.getTotalRatings() * 2, 20); // Max 20 points
        score += Math.min(data.getReadingProgress().getCompletionPercentage() * 0.2, 20); // Max 20 points

        stats.setEngagementScore(score);

        return stats;
    }

    // ============================================
    // HELPER METHODS - Book Analytics
    // ============================================

    private AnalyticsOverview buildAnalyticsOverview(Long bookId, LocalDateTime startDate, LocalDateTime endDate) {
        AnalyticsOverview overview = new AnalyticsOverview();

        // Query database for metrics
        Map<String, Object> metrics = analyticsMapper.getBookOverviewMetrics(bookId, startDate, endDate);

        overview.setTotalReaders(getLongValue(metrics.get("total_readers")));
        overview.setActiveReaders(getLongValue(metrics.get("active_readers")));
        overview.setNewReaders(getLongValue(metrics.get("new_readers")));
        overview.setAverageCompletionRate(getDoubleValue(metrics.get("avg_completion_rate")));
        overview.setAverageReadingTimeMinutes(getIntValue(metrics.get("avg_reading_time")));
        overview.setAverageRating(getDoubleValue(metrics.get("avg_rating")));
        overview.setTotalRatings(getIntValue(metrics.get("total_ratings")));
        overview.setTotalReviews(getIntValue(metrics.get("total_reviews")));

        return overview;
    }

    private ReaderBehaviorAnalytics buildReaderBehaviorAnalytics(Long bookId, LocalDateTime startDate, LocalDateTime endDate) {
        ReaderBehaviorAnalytics behavior = new ReaderBehaviorAnalytics();

        // Device breakdown
        List<Map<String, Object>> deviceData = analyticsMapper.getReadersByDevice(bookId, startDate, endDate);
        behavior.setReadersByDevice(deviceData.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("device_type"),
                        m -> getIntValue(m.get("count"))
                )));

        // Time patterns
        List<Map<String, Object>> hourData = analyticsMapper.getReadingHourDistribution(bookId, startDate, endDate);
        behavior.setReadingHourDistribution(hourData.stream()
                .collect(Collectors.toMap(
                        m -> getIntValue(m.get("hour")),
                        m -> getIntValue(m.get("count"))
                )));

        // Reading patterns
        Map<String, Object> patterns = analyticsMapper.getReadingPatterns(bookId, startDate, endDate);
        behavior.setAverageSessionDurationMinutes(getDoubleValue(patterns.get("avg_session_duration")));
        behavior.setAverageReadingSpeedWpm(getDoubleValue(patterns.get("avg_reading_speed")));
        behavior.setAverageChaptersPerSession(getIntValue(patterns.get("avg_chapters_per_session")));

        // Engagement rates
        Map<String, Object> engagement = analyticsMapper.getEngagementRates(bookId, startDate, endDate);
        behavior.setAnnotationRate(getDoubleValue(engagement.get("annotation_rate")));
        behavior.setRatingRate(getDoubleValue(engagement.get("rating_rate")));
        behavior.setReviewRate(getDoubleValue(engagement.get("review_rate")));

        return behavior;
    }

    private ContentEngagementAnalytics buildContentEngagementAnalytics(Long bookId) {
        ContentEngagementAnalytics engagement = new ContentEngagementAnalytics();

        Map<String, Object> counts = analyticsMapper.getAnnotationCounts(bookId);

        engagement.setTotalAnnotations(getIntValue(counts.get("total_annotations")));
        engagement.setTotalBookmarks(getIntValue(counts.get("total_bookmarks")));
        engagement.setTotalHighlights(getIntValue(counts.get("total_highlights")));
        engagement.setTotalNotes(getIntValue(counts.get("total_notes")));

        // Top engaged chapters
        List<Map<String, Object>> topChapters = analyticsMapper.getTopEngagedChapters(bookId, 10);
        engagement.setTopEngagedChapters(topChapters.stream()
                .map(this::mapToChapterEngagement)
                .collect(Collectors.toList()));

        // Averages
        int totalChapters = chapterMapper.countChaptersByBookId(bookId);
        if (totalChapters > 0) {
            engagement.setAverageAnnotationsPerChapter(
                    engagement.getTotalAnnotations() / (double) totalChapters
            );
        }

        return engagement;
    }

    private List<PopularPassage> findMostHighlightedPassages(Long bookId, int limit) {
        List<Map<String, Object>> data = analyticsMapper.getMostHighlightedPassages(bookId, limit);

        return data.stream()
                .map(this::mapToPopularPassage)
                .collect(Collectors.toList());
    }

    private List<ChapterDropOffPoint> findDropOffPoints(Long bookId) {
        List<Map<String, Object>> data = analyticsMapper.getDropOffPoints(bookId);

        return data.stream()
                .map(this::mapToDropOffPoint)
                .filter(d -> d.getDropOffRate() > 20.0) // Only show significant drop-offs
                .sorted(Comparator.comparing(ChapterDropOffPoint::getDropOffRate).reversed())
                .collect(Collectors.toList());
    }

    private ChapterAnalyticsResponse buildChapterAnalytics(Book book, BookChapter chapter) {
        ChapterAnalyticsResponse analytics = new ChapterAnalyticsResponse();

        analytics.setChapterNumber(chapter.getChapterNumber());
        analytics.setChapterTitle(chapter.getTitle());
        analytics.setChapterSlug(chapter.getSlug());

        // Get chapter stats from materialized view or calculate
        Map<String, Object> stats = analyticsMapper.getChapterStats(book.getId(), chapter.getChapterNumber());

        analytics.setTotalReaders(getLongValue(stats.get("total_readers")));
        analytics.setUniqueReaders(getLongValue(stats.get("unique_readers")));
        analytics.setAverageReadingTimeSeconds(getIntValue(stats.get("avg_reading_time")));
        analytics.setAverageScrollDepth(getDoubleValue(stats.get("avg_scroll_depth")));
        analytics.setCompletionRate(getDoubleValue(stats.get("completion_rate")));
        analytics.setSkipRate(getDoubleValue(stats.get("skip_rate")));
        analytics.setRereadRate(getDoubleValue(stats.get("reread_rate")));
        analytics.setAverageRating(getDoubleValue(stats.get("avg_rating")));
        analytics.setTotalRatings(getIntValue(stats.get("total_ratings")));
        analytics.setTotalBookmarks(getIntValue(stats.get("total_bookmarks")));
        analytics.setTotalHighlights(getIntValue(stats.get("total_highlights")));
        analytics.setTotalNotes(getIntValue(stats.get("total_notes")));
        analytics.setTotalComments(getIntValue(stats.get("total_comments")));

        // Calculate engagement score
        analytics.setEngagementScore(calculateEngagementScore(analytics));
        analytics.setPopularityLevel(determinePopularityLevel(analytics));
        analytics.setDifficultyLevel(determineDifficultyLevel(analytics));

        // Top highlights
        analytics.setTopHighlights(findTopHighlightsForChapter(book.getId(), chapter.getChapterNumber()));

        return analytics;
    }

    // ============================================
    // MAPPING HELPERS
    // ============================================

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

    private List<BookmarkResponse> getBookmarksList(Long userId, Long bookId) {
        // Implementation from existing service
        return bookmarkMapper.findByUserBookAndPage(userId, bookId, null).stream()
                .map(b -> {
                    BookmarkResponse r = new BookmarkResponse();
                    r.setId(b.getId());
                    r.setBookId(b.getBookId());
                    r.setPage(b.getChapterNumber());
                    r.setPosition(b.getPosition());
                    r.setTitle(b.getTitle());
                    r.setDescription(b.getDescription());
                    r.setColor(b.getColor());
                    r.setCreatedAt(b.getCreatedAt());
                    return r;
                })
                .collect(Collectors.toList());
    }

    private List<HighlightResponse> getHighlightsList(Long userId, Long bookId) {
        return highlightMapper.findByUserBookAndPage(userId, bookId, null).stream()
                .map(h -> {
                    HighlightResponse r = new HighlightResponse();
                    r.setId(h.getId());
                    r.setBookId(h.getBookId());
                    r.setPage(h.getChapterNumber());
                    r.setStartPosition(h.getStartPosition());
                    r.setEndPosition(h.getEndPosition());
                    r.setHighlightedText(h.getHighlightedText());
                    r.setColor(h.getColor());
                    r.setNote(h.getNote());
                    r.setCreatedAt(h.getCreatedAt());
                    r.setUpdatedAt(h.getUpdatedAt());
                    return r;
                })
                .collect(Collectors.toList());
    }

    private List<NoteResponse> getNotesList(Long userId, Long bookId) {
        return noteMapper.findByUserBookAndPage(userId, bookId, null).stream()
                .map(n -> {
                    NoteResponse r = new NoteResponse();
                    r.setId(n.getId());
                    r.setBookId(n.getBookId());
                    r.setPage(n.getChapterNumber());
                    r.setPosition(n.getPosition());
                    r.setTitle(n.getTitle());
                    r.setContent(n.getContent());
                    r.setColor(n.getColor());
                    r.setIsPrivate(n.getIsPrivate());
                    r.setCreatedAt(n.getCreatedAt());
                    r.setUpdatedAt(n.getUpdatedAt());
                    return r;
                })
                .collect(Collectors.toList());
    }

    // ============================================
// ANALYTICS SERVICE - CONTINUATION & HELPERS
// ============================================

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

    private int calculateCurrentStreak(List<ReadingSession> sessions) {
        if (sessions.isEmpty()) return 0;

        // Sort by date descending
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

    private int calculateLongestStreak(List<ReadingSession> sessions) {
        if (sessions.isEmpty()) return 0;

        List<LocalDateTime> dates = sessions.stream()
                .map(ReadingSession::getStartedAt)
                .map(dt -> dt.toLocalDate().atStartOfDay())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

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

    private ChapterEngagement mapToChapterEngagement(Map<String, Object> data) {
        ChapterEngagement engagement = new ChapterEngagement();
        engagement.setChapterNumber(getIntValue(data.get("chapter_number")));
        engagement.setChapterTitle((String) data.get("chapter_title"));
        engagement.setAnnotationCount(getIntValue(data.get("annotation_count")));
        engagement.setEngagementScore(getDoubleValue(data.get("engagement_score")));
        engagement.setUniqueReaders(getIntValue(data.get("unique_readers")));
        return engagement;
    }

    private PopularPassage mapToPopularPassage(Map<String, Object> data) {
        PopularPassage passage = new PopularPassage();
        passage.setId(getLongValue(data.get("id")));
        passage.setChapterNumber(getIntValue(data.get("chapter_number")));
        passage.setChapterTitle((String) data.get("chapter_title"));
        passage.setPassage((String) data.get("passage"));
        passage.setStartPosition(getIntValue(data.get("start_position")));
        passage.setEndPosition(getIntValue(data.get("end_position")));
        passage.setHighlightCount(getIntValue(data.get("highlight_count")));
        passage.setPopularityScore(getDoubleValue(data.get("popularity_score")));

        // Parse array of colors
        Object colorsObj = data.get("common_colors");
        if (colorsObj != null) {
            // Handle PostgreSQL array
            passage.setCommonColors(parseArray(colorsObj));
        }

        return passage;
    }

    private ChapterDropOffPoint mapToDropOffPoint(Map<String, Object> data) {
        ChapterDropOffPoint point = new ChapterDropOffPoint();
        point.setChapterNumber(getIntValue(data.get("chapter_number")));
        point.setChapterTitle((String) data.get("chapter_title"));
        point.setDropOffRate(getDoubleValue(data.get("drop_off_rate")));
        point.setAverageScrollDepth(getIntValue(data.get("average_scroll_depth")));
        point.setReadersStarted(getIntValue(data.get("readers_started")));
        point.setReadersCompleted(getIntValue(data.get("readers_completed")));

        // Determine severity
        double dropOffRate = point.getDropOffRate();
        if (dropOffRate >= 50) point.setSeverity("Critical");
        else if (dropOffRate >= 35) point.setSeverity("High");
        else if (dropOffRate >= 20) point.setSeverity("Medium");
        else point.setSeverity("Low");

        return point;
    }

    private List<PopularNote> findMostCommonNotes(Long bookId, int limit) {
        // This would require NLP/text analysis to group notes by theme
        // For now, return empty list
        // TODO: Implement note clustering/theme extraction
        return new ArrayList<>();
    }

    private List<ChapterSkipAnalysis> findMostSkippedChapters(Long bookId) {
        List<Map<String, Object>> data = analyticsMapper.getMostSkippedChapters(bookId);

        return data.stream()
                .map(d -> {
                    ChapterSkipAnalysis analysis = new ChapterSkipAnalysis();
                    analysis.setChapterNumber(getIntValue(d.get("chapter_number")));
                    analysis.setChapterTitle((String) d.get("chapter_title"));
                    analysis.setSkipRate(getDoubleValue(d.get("skip_rate")));
                    analysis.setTimesSkipped(getIntValue(d.get("times_skipped")));
                    analysis.setTotalReaders(getIntValue(d.get("total_readers")));

                    // Infer possible reason
                    analysis.setPossibleReason(inferSkipReason(analysis));

                    return analysis;
                })
                .collect(Collectors.toList());
    }

    private String inferSkipReason(ChapterSkipAnalysis analysis) {
        // Simple heuristic - could be improved with ML
        if (analysis.getSkipRate() > 50) {
            return "Critical engagement issue - needs review";
        } else if (analysis.getSkipRate() > 30) {
            return "Possibly too long or complex";
        } else {
            return "Normal skip rate";
        }
    }

    private TrendAnalysis analyzeTrends(Long bookId, LocalDateTime startDate, LocalDateTime endDate) {
        TrendAnalysis trends = new TrendAnalysis();

        // Compare current period with previous period
        LocalDateTime previousStart = startDate.minusDays(
                java.time.Duration.between(startDate, endDate).toDays()
        );

        Map<String, Object> currentMetrics = analyticsMapper.getBookOverviewMetrics(
                bookId, startDate, endDate
        );
        Map<String, Object> previousMetrics = analyticsMapper.getBookOverviewMetrics(
                bookId, previousStart, startDate
        );

        // Calculate changes
        long currentReaders = getLongValue(currentMetrics.get("total_readers"));
        long previousReaders = getLongValue(previousMetrics.get("total_readers"));

        if (previousReaders > 0) {
            int readerChange = (int) (((currentReaders - previousReaders) * 100.0) / previousReaders);
            trends.setReadersChangePercentage(readerChange);

            if (readerChange > 10) trends.setReadersGrowth("increasing");
            else if (readerChange < -10) trends.setReadersGrowth("decreasing");
            else trends.setReadersGrowth("stable");
        }

        // Engagement trend (based on annotation rate)
        // Similar calculation...
        trends.setEngagementTrend("stable"); // Placeholder
        trends.setPopularityTrend("stable"); // Placeholder

        // Predictions (simple linear extrapolation)
        if (currentReaders > previousReaders) {
            long growth = currentReaders - previousReaders;
            trends.setEstimatedReadersNextMonth((int) (currentReaders + growth));
        } else {
            trends.setEstimatedReadersNextMonth((int) currentReaders);
        }

        double currentCompletion = getDoubleValue(currentMetrics.get("avg_completion_rate"));
        trends.setEstimatedCompletionRate(currentCompletion);

        return trends;
    }

    private List<PopularHighlight> findTopHighlightsForChapter(Long bookId, Integer chapterNumber) {
        List<Map<String, Object>> data = analyticsMapper.getTopHighlightsForChapter(bookId, chapterNumber);

        return data.stream()
                .map(d -> {
                    PopularHighlight highlight = new PopularHighlight();
                    highlight.setText((String) d.get("text"));
                    highlight.setHighlightCount(getIntValue(d.get("highlight_count")));
                    highlight.setPosition(getIntValue(d.get("position")));
                    return highlight;
                })
                .collect(Collectors.toList());
    }

    private int calculateEngagementScore(ChapterAnalyticsResponse analytics) {
        // Weighted score calculation
        int score = 0;

        // Reading completion (30 points)
        score += Math.min(analytics.getCompletionRate() * 0.3, 30);

        // Annotations (25 points)
        int totalAnnotations = analytics.getTotalBookmarks() +
                analytics.getTotalHighlights() +
                analytics.getTotalNotes();
        score += Math.min(totalAnnotations * 0.5, 25);

        // Rating (20 points)
        if (analytics.getAverageRating() != null) {
            score += (analytics.getAverageRating() / 5.0) * 20;
        }

        // Comments/discussions (15 points)
        score += Math.min(analytics.getTotalComments() * 0.3, 15);

        // Low skip rate bonus (10 points)
        if (analytics.getSkipRate() != null) {
            score += Math.max(0, 10 - (analytics.getSkipRate() * 0.2));
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
        // Based on skip rate, reading time, completion rate
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

        if (readingTime > 1800) difficultyScore += 3; // > 30 min
        else if (readingTime > 900) difficultyScore += 2; // > 15 min
        else difficultyScore += 1;

        if (difficultyScore >= 8) return "Very Difficult";
        if (difficultyScore >= 6) return "Difficult";
        if (difficultyScore >= 4) return "Moderate";
        return "Easy";
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

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


    private List<String> parseArray(Object arrayObj) {
        // Handle PostgreSQL array conversion
        if (arrayObj == null) return new ArrayList<>();

        if (arrayObj instanceof String[]) {
            return Arrays.asList((String[]) arrayObj);
        }

        if (arrayObj instanceof List) {
            return (List<String>) arrayObj;
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