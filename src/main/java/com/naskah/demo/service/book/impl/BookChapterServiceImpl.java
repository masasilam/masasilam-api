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
            bookmark.setPage(chapterNumber);
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
            highlight.setPage(chapterNumber);
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
            note.setPage(chapterNumber);
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
                .map(Bookmark::getPage)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // ✅ Batch fetch all chapter info in one query
        Map<Integer, ChapterNavigationInfo> chapterInfoMap = getChapterInfoMap(bookId, chapterNumbers);

        // ✅ Map to response with chapter info
        return bookmarks.stream()
                .map(bookmark -> {
                    BookmarkResponse response = entityMapper.toBookmarkResponse(bookmark);
                    ChapterNavigationInfo chapterInfo = chapterInfoMap.get(bookmark.getPage());
                    response.setChapter(chapterInfo);
                    return response;
                })
                .collect(Collectors.toList());
    }

    private List<HighlightResponse> getUserChapterHighlights(Long userId, Long bookId, Integer chapterNumber) {
        List<Highlight> highlights = highlightMapper.findByUserBookAndPage(userId, bookId, chapterNumber);

        Set<Integer> chapterNumbers = highlights.stream()
                .map(Highlight::getPage)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, ChapterNavigationInfo> chapterInfoMap = getChapterInfoMap(bookId, chapterNumbers);

        return highlights.stream()
                .map(highlight -> {
                    HighlightResponse response = entityMapper.toHighlightResponse(highlight);
                    ChapterNavigationInfo chapterInfo = chapterInfoMap.get(highlight.getPage());
                    response.setChapter(chapterInfo);
                    return response;
                })
                .collect(Collectors.toList());
    }

    private List<NoteResponse> getUserChapterNotes(Long userId, Long bookId, Integer chapterNumber) {
        List<Note> notes = noteMapper.findByUserBookAndPage(userId, bookId, chapterNumber);

        Set<Integer> chapterNumbers = notes.stream()
                .map(Note::getPage)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, ChapterNavigationInfo> chapterInfoMap = getChapterInfoMap(bookId, chapterNumbers);

        return notes.stream()
                .map(note -> {
                    NoteResponse response = entityMapper.toNoteResponse(note);
                    ChapterNavigationInfo chapterInfo = chapterInfoMap.get(note.getPage());
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
}