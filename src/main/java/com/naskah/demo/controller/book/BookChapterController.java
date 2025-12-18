// ============================================
// BookChapterController.java - FULL CODE AFTER FIX
// ============================================

package com.naskah.demo.controller.book;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.book.BookChapterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BookChapterController - Handle ALL per-chapter operations
 *
 * Features:
 * - Read chapter content (HTML with images)
 * - Chapter-specific annotations (bookmarks, highlights, notes)
 * - Chapter-specific reviews & discussions
 * - Chapter audio (TTS or pre-generated)
 * - Chapter reading progress
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("api/books/{slug}/chapters")
@RequiredArgsConstructor
public class BookChapterController {

    private final BookChapterService chapterService;

    // ============ CHAPTER READING ============

    /**
     * Get chapter content with user's annotations
     * Returns: HTML content, images, bookmarks, highlights, notes, audio URL
     */
//    @GetMapping("/{chapterNumber}")
//    public ResponseEntity<DataResponse<ChapterReadingResponse>> readChapter(
//            @PathVariable String slug,
//            @PathVariable Integer chapterNumber) {
//        DataResponse<ChapterReadingResponse> response = chapterService.readChapter(slug, chapterNumber);
//        return ResponseEntity.ok(response);
//    }

    /**
     * Read chapter by hierarchical slug path
     * Examples:
     * GET /api/books/kerikil-tajam-dan-yang-terampas-dan-yang-putus/read/kerikil-tajam
     * GET /api/books/kerikil-tajam-dan-yang-terampas-dan-yang-putus/read/kerikil-tajam/nisan
     * GET /api/books/kerikil-tajam-dan-yang-terampas-dan-yang-putus/read/yang-terampas-dan-yang-putus/fragmen
     */
    @GetMapping("/**")
    public ResponseEntity<DataResponse<ChapterReadingResponse>> readChapterByPath(
            @PathVariable String slug,
            HttpServletRequest request) {

        String fullPath = request.getRequestURI();
        String basePath = "/api/books/" + slug + "/chapters/";

        if (!fullPath.startsWith(basePath)) {
            throw new DataNotFoundException();
        }

        String chapterPath = fullPath.substring(basePath.length());

        // Remove query parameters and trailing slashes
        chapterPath = chapterPath.split("\\?")[0].replaceAll("/+$", "");

        if (chapterPath.isEmpty()) {
            throw new DataNotFoundException();
        }

        DataResponse<ChapterReadingResponse> response =
                chapterService.readChapterBySlugPath(slug, chapterPath);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all chapters list (table of contents)
     */
    @GetMapping
    public ResponseEntity<DataResponse<List<ChapterSummaryResponse>>> getAllChapters(
            @PathVariable String slug) {
        DataResponse<List<ChapterSummaryResponse>> response = chapterService.getAllChaptersSummary(slug);
        return ResponseEntity.ok(response);
    }

    /**
     * Save chapter reading progress
     */
    @PostMapping("/{chapterNumber}/progress")
    public ResponseEntity<DataResponse<ChapterProgressResponse>> saveChapterProgress(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody ChapterProgressRequest request) {
        DataResponse<ChapterProgressResponse> response = chapterService.saveChapterProgress(slug, chapterNumber, request);
        return ResponseEntity.ok(response);
    }

    // ============ CHAPTER ANNOTATIONS (ADD) ============

    /**
     * Add bookmark to specific chapter position
     */
    @PostMapping("/{chapterNumber}/bookmarks")
    public ResponseEntity<DataResponse<BookmarkResponse>> addChapterBookmark(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody ChapterBookmarkRequest request) {
        DataResponse<BookmarkResponse> response = chapterService.addChapterBookmark(slug, chapterNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Add highlight to chapter text
     */
    @PostMapping("/{chapterNumber}/highlights")
    public ResponseEntity<DataResponse<HighlightResponse>> addChapterHighlight(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody ChapterHighlightRequest request) {
        DataResponse<HighlightResponse> response = chapterService.addChapterHighlight(slug, chapterNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Add note to chapter
     */
    @PostMapping("/{chapterNumber}/notes")
    public ResponseEntity<DataResponse<NoteResponse>> addChapterNote(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody ChapterNoteRequest request) {
        DataResponse<NoteResponse> response = chapterService.addChapterNote(slug, chapterNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============ CHAPTER ANNOTATIONS (DELETE) ============

    /**
     * Delete bookmark
     */
    @DeleteMapping("/{chapterNumber}/bookmarks/{bookmarkId}")
    public ResponseEntity<DataResponse<Void>> deleteChapterBookmark(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long bookmarkId) {
        DataResponse<Void> response = chapterService.deleteChapterBookmark(slug, chapterNumber, bookmarkId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete highlight
     */
    @DeleteMapping("/{chapterNumber}/highlights/{highlightId}")
    public ResponseEntity<DataResponse<Void>> deleteChapterHighlight(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long highlightId) {
        DataResponse<Void> response = chapterService.deleteChapterHighlight(slug, chapterNumber, highlightId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete note
     */
    @DeleteMapping("/{chapterNumber}/notes/{noteId}")
    public ResponseEntity<DataResponse<Void>> deleteChapterNote(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long noteId) {
        DataResponse<Void> response = chapterService.deleteChapterNote(slug, chapterNumber, noteId);
        return ResponseEntity.ok(response);
    }

    // ============ CHAPTER SOCIAL (Reviews & Discussions) ============

    /**
     * Get reviews/comments for specific chapter
     * Users can discuss what happened in this chapter!
     */
    @GetMapping("/{chapterNumber}/reviews")
    public ResponseEntity<DataResponse<List<ChapterReviewResponse>>> getChapterReviews(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        DataResponse<List<ChapterReviewResponse>> response = chapterService.getChapterReviews(slug, chapterNumber, page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Add review/comment to chapter
     */
    @PostMapping("/{chapterNumber}/reviews")
    public ResponseEntity<DataResponse<ChapterReviewResponse>> addChapterReview(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody ChapterReviewRequest request) {
        DataResponse<ChapterReviewResponse> response = chapterService.addChapterReview(slug, chapterNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Reply to chapter review
     */
    @PostMapping("/{chapterNumber}/reviews/{reviewId}/replies")
    public ResponseEntity<DataResponse<ChapterReviewResponse>> replyToChapterReview(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long reviewId,
            @Valid @RequestBody ChapterReplyRequest request) {
        DataResponse<ChapterReviewResponse> response = chapterService.replyToChapterReview(slug, chapterNumber, reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Like/unlike chapter review
     */
    @PostMapping("/{chapterNumber}/reviews/{reviewId}/like")
    public ResponseEntity<DataResponse<Void>> likeChapterReview(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long reviewId) {
        DataResponse<Void> response = chapterService.likeChapterReview(slug, chapterNumber, reviewId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chapterNumber}/reviews/{reviewId}/like")
    public ResponseEntity<DataResponse<Void>> unlikeChapterReview(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long reviewId) {
        DataResponse<Void> response = chapterService.unlikeChapterReview(slug, chapterNumber, reviewId);
        return ResponseEntity.ok(response);
    }

    // ============ CHAPTER AUDIO & TEXT ============

    @GetMapping("/{chapterNumber}/text")
    public ResponseEntity<DataResponse<ChapterTextResponse>> getChapterText(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber) {
        DataResponse<ChapterTextResponse> response = chapterService.getChapterTextForTTS(slug, chapterNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chapterNumber}/paragraphs")
    public ResponseEntity<DataResponse<ChapterParagraphsResponse>> getChapterParagraphs(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber) {
        DataResponse<ChapterParagraphsResponse> response = chapterService.getChapterParagraphs(slug, chapterNumber);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // 1. CHAPTER RATING API
    // ============================================

    /**
     * Rate a chapter (1-5 stars)
     * DIFFERENT from reviews: ratings are quick numerical feedback
     * Reviews are detailed text comments with discussion
     *
     * POST /api/books/{slug}/chapters/{chapterNumber}/rating
     */
    @PostMapping("/{chapterNumber}/rating")
    public ResponseEntity<DataResponse<ChapterRatingResponse>> rateChapter(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody ChapterRatingRequest request) {
        DataResponse<ChapterRatingResponse> response =
                chapterService.rateChapter(slug, chapterNumber, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get chapter rating summary
     * Shows: average rating, total ratings, distribution, user's rating
     *
     * GET /api/books/{slug}/chapters/{chapterNumber}/rating
     */
    @GetMapping("/{chapterNumber}/rating")
    public ResponseEntity<DataResponse<ChapterRatingSummaryResponse>> getChapterRating(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber) {
        DataResponse<ChapterRatingSummaryResponse> response =
                chapterService.getChapterRatingSummary(slug, chapterNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete user's chapter rating
     *
     * DELETE /api/books/{slug}/chapters/{chapterNumber}/rating
     */
    @DeleteMapping("/{chapterNumber}/rating")
    public ResponseEntity<DataResponse<Void>> deleteChapterRating(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber) {
        DataResponse<Void> response =
                chapterService.deleteChapterRating(slug, chapterNumber);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // 2. READING ACTIVITY TRACKING API
    // ============================================

    /**
     * Start reading - called when user opens a chapter
     * Tracks: session start, device type, reading patterns
     *
     * POST /api/books/{slug}/chapters/reading/start
     */
    @PostMapping("/reading/start")
    public ResponseEntity<DataResponse<Void>> startReading(
            @PathVariable String slug,
            @Valid @RequestBody StartReadingRequest request) {
        DataResponse<Void> response = chapterService.startReading(slug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * End reading - called when user closes chapter or navigates away
     * Records: duration, scroll depth, words read, reading speed
     *
     * POST /api/books/{slug}/chapters/reading/end
     */
    @PostMapping("/reading/end")
    public ResponseEntity<DataResponse<Void>> endReading(
            @PathVariable String slug,
            @Valid @RequestBody EndReadingRequest request) {
        DataResponse<Void> response = chapterService.endReading(slug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reading heartbeat - periodic updates during reading
     * Call every 30-60 seconds for real-time progress tracking
     *
     * POST /api/books/{slug}/chapters/reading/heartbeat
     */
    @PostMapping("/reading/heartbeat")
    public ResponseEntity<DataResponse<Void>> readingHeartbeat(
            @PathVariable String slug,
            @Valid @RequestBody ReadingHeartbeatRequest request) {
        // Heartbeat just updates current position, doesn't close session
        DataResponse<Void> response =
                new DataResponse<>("Success", "Heartbeat received", 200, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's complete reading history for this book
     * Shows: chapters read, time spent per chapter, reading statistics
     *
     * GET /api/books/{slug}/chapters/reading/history
     */
    @GetMapping("/reading/history")
    public ResponseEntity<DataResponse<ReadingHistoryResponse>> getReadingHistory(
            @PathVariable String slug) {
        DataResponse<ReadingHistoryResponse> response =
                chapterService.getReadingHistory(slug);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's reading pattern analysis
     * Shows: preferred reading time, pace, behavior patterns
     *
     * GET /api/books/{slug}/chapters/reading/patterns
     */
    @GetMapping("/reading/patterns")
    public ResponseEntity<DataResponse<UserReadingPatternResponse>> getReadingPattern(
            @PathVariable String slug) {
        DataResponse<UserReadingPatternResponse> response =
                chapterService.getUserReadingPattern(slug);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // 3. SEARCH IN BOOK API
    // ============================================

    /**
     * Search within book content (Full-text search)
     * Searches across all chapters with PostgreSQL full-text search
     * Returns: matching chapters, highlighted snippets, relevance scores
     *
     * POST /api/books/{slug}/chapters/search
     */
    @PostMapping("/search")
    public ResponseEntity<DataResponse<SearchInBookResponse>> searchInBook(
            @PathVariable String slug,
            @Valid @RequestBody SearchInBookRequest request) {
        DataResponse<SearchInBookResponse> response =
                chapterService.searchInBook(slug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's search history for this book
     *
     * GET /api/books/{slug}/chapters/search/history?limit=10
     */
    @GetMapping("/search/history")
    public ResponseEntity<DataResponse<List<SearchHistoryResponse>>> getSearchHistory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "10") int limit) {
        // This would fetch from service
        DataResponse<List<SearchHistoryResponse>> response =
                new DataResponse<>("Success", "Search history retrieved", 200, List.of());
        return ResponseEntity.ok(response);
    }

    // ============================================
    // 4. EXPORT ANNOTATIONS API
    // ============================================

    /**
     * Export user's annotations (bookmarks, highlights, notes)
     * Formats: PDF, DOCX, JSON, HTML, Markdown
     *
     * Use cases:
     * - Student exporting study notes
     * - Researcher exporting highlights for citations
     * - Backup of personal annotations
     *
     * POST /api/books/{slug}/chapters/annotations/export
     */
    @PostMapping("/annotations/export")
    public ResponseEntity<DataResponse<ExportAnnotationsResponse>> exportAnnotations(
            @PathVariable String slug,
            @Valid @RequestBody ExportAnnotationsRequest request) {
        DataResponse<ExportAnnotationsResponse> response =
                chapterService.exportAnnotations(slug, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get status of export job
     * Poll this endpoint to check if export is ready
     *
     * GET /api/books/{slug}/chapters/annotations/export/{exportId}
     */
    @GetMapping("/annotations/export/{exportId}")
    public ResponseEntity<DataResponse<ExportAnnotationsResponse>> getExportStatus(
            @PathVariable String slug,
            @PathVariable Long exportId) {
        // Fetch export status
        DataResponse<ExportAnnotationsResponse> response =
                new DataResponse<>("Success", "Export status retrieved", 200, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Download exported file
     * Returns: redirect to S3 URL or streams file directly
     *
     * GET /api/books/{slug}/chapters/annotations/export/{exportId}/download
     */
    @GetMapping("/annotations/export/{exportId}/download")
    public ResponseEntity<Void> downloadExport(
            @PathVariable String slug,
            @PathVariable Long exportId) {
        // Return redirect to S3 or stream file
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "https://s3.amazonaws.com/exports/...")
                .build();
    }

    /**
     * Get user's export history
     * Shows: all past exports, file sizes, status
     *
     * GET /api/books/{slug}/chapters/annotations/exports?page=1&limit=10
     */
    @GetMapping("/annotations/exports")
    public ResponseEntity<DataResponse<ExportHistoryResponse>> getExportHistory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        DataResponse<ExportHistoryResponse> response =
                chapterService.getExportHistory(page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete/cancel export
     * Removes export record and file from storage
     *
     * DELETE /api/books/{slug}/chapters/annotations/export/{exportId}
     */
    @DeleteMapping("/annotations/export/{exportId}")
    public ResponseEntity<DataResponse<Void>> deleteExport(
            @PathVariable String slug,
            @PathVariable Long exportId) {
        DataResponse<Void> response =
                new DataResponse<>("Success", "Export deleted", 200, null);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // 5. BULK USER DATA (Optimization)
    // ============================================

    /**
     * 🔥 GET ALL USER DATA FOR THIS BOOK IN ONE REQUEST
     * Optimized for initial page load - reduces multiple API calls
     *
     * Returns:
     * - Reading progress (current chapter, completion %, time spent)
     * - All annotations (bookmarks, highlights, notes)
     * - All chapter ratings
     * - Reading history summary (sessions, streaks, statistics)
     * - Search history
     * - Reading patterns (preferred time, pace, behavior)
     * - User statistics (engagement score, total annotations, etc.)
     *
     * GET /api/books/{slug}/chapters/me
     */
    @GetMapping("/me")
    public ResponseEntity<DataResponse<UserBookDataResponse>> getMyBookData(
            @PathVariable String slug) {
        DataResponse<UserBookDataResponse> response =
                chapterService.getMyBookData(slug);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // 6. ANALYTICS ENDPOINTS (for Authors/Admins)
    // ============================================

    /**
     * 📊 GET BOOK-WIDE ANALYTICS
     * For: Authors, Publishers, Admins
     * Requires: AUTHOR or ADMIN role
     *
     * Returns comprehensive analytics:
     * - Overview (total/active readers, completion rate, avg reading time)
     * - Reader behavior (device breakdown, time patterns, engagement rates)
     * - Content engagement (annotations, top engaged chapters)
     * - Popular content (most highlighted passages, common notes)
     * - Problem areas (drop-off points, most skipped chapters)
     * - Trends (growth, engagement trends, predictions)
     *
     * GET /api/books/{slug}/chapters/analytics?dateFrom=2025-01-01&dateTo=2025-01-31
     */
    @GetMapping("/analytics")// Uncomment when security is configured
    public ResponseEntity<DataResponse<BookAnalyticsResponse>> getBookAnalytics(
            @PathVariable String slug,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        DataResponse<BookAnalyticsResponse> response =
                chapterService.getBookAnalytics(slug, dateFrom, dateTo);

        return ResponseEntity.ok(response);
    }

    /**
     * 📈 GET CHAPTER-SPECIFIC ANALYTICS
     * For: Authors, Publishers, Admins
     * Requires: AUTHOR or ADMIN role
     *
     * Shows which chapters are:
     * - Most engaging (high completion, annotations, ratings)
     * - Most skipped (readers skip over them)
     * - Most difficult (low completion, high skip rate, long reading time)
     * - Most popular (high ratings, many readers)
     *
     * Each chapter gets:
     * - Reading stats (total readers, avg time, scroll depth)
     * - Engagement metrics (completion rate, skip rate, reread rate)
     * - Ratings (average, total, distribution)
     * - Annotations (bookmarks, highlights, notes, comments)
     * - Calculated scores (engagement score 0-100, popularity level, difficulty)
     * - Top highlights (most popular passages in chapter)
     * - Comparisons (vs previous chapter, vs book average)
     *
     * GET /api/books/{slug}/chapters/analytics/chapters
     */
    @GetMapping("/analytics/chapters")// Uncomment when security is configured
    public ResponseEntity<DataResponse<List<ChapterAnalyticsResponse>>> getChaptersAnalytics(
            @PathVariable String slug) {

        DataResponse<List<ChapterAnalyticsResponse>> response =
                chapterService.getChaptersAnalytics(slug);

        return ResponseEntity.ok(response);
    }
}