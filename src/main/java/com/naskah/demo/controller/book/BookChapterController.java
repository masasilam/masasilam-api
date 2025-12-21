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
 * ✅ FULLY SYNCHRONIZED WITH DASHBOARD
 *
 * Features:
 * - Read chapter content (HTML with images)
 * - Chapter-specific annotations (bookmarks, highlights, notes)
 * - Chapter-specific reviews & discussions
 * - Chapter audio (TTS or pre-generated)
 * - Chapter reading progress
 * - Reading activity tracking (for Dashboard statistics)
 * - User book data aggregation (for Dashboard library)
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/books/{slug}/chapters")
@RequiredArgsConstructor
public class BookChapterController {

    private final BookChapterService chapterService;

    // ============================================
    // CHAPTER READING
    // ============================================

    /**
     * Read chapter by hierarchical slug path
     *
     * Examples:
     * GET /api/books/kerikil-tajam-dan-yang-terampas-dan-yang-putus/chapters/kerikil-tajam
     * GET /api/books/kerikil-tajam-dan-yang-terampas-dan-yang-putus/chapters/kerikil-tajam/nisan
     *
     * ✅ Connected to Dashboard: Updates last_read_at, reading_sessions
     */
    @GetMapping("/**")
    public ResponseEntity<DataResponse<ChapterReadingResponse>> readChapterByPath(@PathVariable String slug, HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String basePath = "/api/books/" + slug + "/chapters/";

        if (!fullPath.startsWith(basePath)) {
            throw new DataNotFoundException();
        }

        String chapterPath = fullPath.substring(basePath.length());
        chapterPath = chapterPath.split("\\?")[0].replaceAll("/+$", "");

        if (chapterPath.isEmpty()) {
            throw new DataNotFoundException();
        }

        DataResponse<ChapterReadingResponse> response = chapterService.readChapterBySlugPath(slug, chapterPath);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all chapters list (table of contents)
     *
     * ✅ Connected to Dashboard: Shows completion status
     */
    @GetMapping
    public ResponseEntity<DataResponse<List<ChapterSummaryResponse>>> getAllChapters(@PathVariable String slug) {
        DataResponse<List<ChapterSummaryResponse>> response = chapterService.getAllChaptersSummary(slug);

        return ResponseEntity.ok(response);
    }

    /**
     * Save chapter reading progress
     *
     * ✅ Connected to Dashboard: Updates library progress, completion stats
     */
    @PostMapping("/{chapterNumber}/progress")
    public ResponseEntity<DataResponse<ChapterProgressResponse>> saveChapterProgress(@PathVariable String slug, @PathVariable Integer chapterNumber,
                                                                                     @Valid @RequestBody ChapterProgressRequest request) {
        DataResponse<ChapterProgressResponse> response = chapterService.saveChapterProgress(slug, chapterNumber, request);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // CHAPTER ANNOTATIONS (ADD)
    // ✅ Connected to Dashboard: Shows in recent annotations, annotations summary
    // ============================================

    /**
     * Add bookmark to specific chapter position
     */
    @PostMapping("/{chapterNumber}/bookmarks")
    public ResponseEntity<DataResponse<BookmarkResponse>> addChapterBookmark(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody ChapterBookmarkRequest request) {
        DataResponse<BookmarkResponse> response =
                chapterService.addChapterBookmark(slug, chapterNumber, request);
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
        DataResponse<HighlightResponse> response =
                chapterService.addChapterHighlight(slug, chapterNumber, request);
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
        DataResponse<NoteResponse> response =
                chapterService.addChapterNote(slug, chapterNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================
    // CHAPTER ANNOTATIONS (DELETE)
    // ✅ Connected to Dashboard: Updates annotation counts
    // ============================================

    @DeleteMapping("/{chapterNumber}/bookmarks/{bookmarkId}")
    public ResponseEntity<DataResponse<Void>> deleteChapterBookmark(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long bookmarkId) {
        DataResponse<Void> response =
                chapterService.deleteChapterBookmark(slug, chapterNumber, bookmarkId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chapterNumber}/highlights/{highlightId}")
    public ResponseEntity<DataResponse<Void>> deleteChapterHighlight(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long highlightId) {
        DataResponse<Void> response =
                chapterService.deleteChapterHighlight(slug, chapterNumber, highlightId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chapterNumber}/notes/{noteId}")
    public ResponseEntity<DataResponse<Void>> deleteChapterNote(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long noteId) {
        DataResponse<Void> response =
                chapterService.deleteChapterNote(slug, chapterNumber, noteId);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // CHAPTER SOCIAL (Reviews & Discussions)
    // ✅ Connected to Dashboard: Shows in recent activity, review counts
    // ============================================

    @GetMapping("/{chapterNumber}/reviews")
    public ResponseEntity<DataResponse<List<ChapterReviewResponse>>> getChapterReviews(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        DataResponse<List<ChapterReviewResponse>> response =
                chapterService.getChapterReviews(slug, chapterNumber, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{chapterNumber}/reviews")
    public ResponseEntity<DataResponse<ChapterReviewResponse>> addChapterReview(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody ChapterReviewRequest request) {
        DataResponse<ChapterReviewResponse> response = chapterService.addChapterReview(slug, chapterNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{chapterNumber}/reviews/{reviewId}/replies")
    public ResponseEntity<DataResponse<ChapterReviewResponse>> replyToChapterReview(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long reviewId,
            @Valid @RequestBody ChapterReplyRequest request) {
        DataResponse<ChapterReviewResponse> response =
                chapterService.replyToChapterReview(slug, chapterNumber, reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{chapterNumber}/reviews/{reviewId}/like")
    public ResponseEntity<DataResponse<Void>> likeChapterReview(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long reviewId) {
        DataResponse<Void> response =
                chapterService.likeChapterReview(slug, chapterNumber, reviewId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chapterNumber}/reviews/{reviewId}/like")
    public ResponseEntity<DataResponse<Void>> unlikeChapterReview(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @PathVariable Long reviewId) {
        DataResponse<Void> response =
                chapterService.unlikeChapterReview(slug, chapterNumber, reviewId);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // CHAPTER AUDIO & TEXT
    // ============================================

    @GetMapping("/{chapterNumber}/text")
    public ResponseEntity<DataResponse<ChapterTextResponse>> getChapterText(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber) {
        DataResponse<ChapterTextResponse> response =
                chapterService.getChapterTextForTTS(slug, chapterNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chapterNumber}/paragraphs")
    public ResponseEntity<DataResponse<ChapterParagraphsResponse>> getChapterParagraphs(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber) {
        DataResponse<ChapterParagraphsResponse> response =
                chapterService.getChapterParagraphs(slug, chapterNumber);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // CHAPTER RATING
    // ✅ Connected to Dashboard: Contributes to average rating stats
    // ============================================

    @PostMapping("/{chapterNumber}/rating")
    public ResponseEntity<DataResponse<ChapterRatingResponse>> rateChapter(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber,
            @Valid @RequestBody ChapterRatingRequest request) {
        DataResponse<ChapterRatingResponse> response =
                chapterService.rateChapter(slug, chapterNumber, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chapterNumber}/rating")
    public ResponseEntity<DataResponse<ChapterRatingSummaryResponse>> getChapterRating(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber) {
        DataResponse<ChapterRatingSummaryResponse> response =
                chapterService.getChapterRatingSummary(slug, chapterNumber);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chapterNumber}/rating")
    public ResponseEntity<DataResponse<Void>> deleteChapterRating(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber) {
        DataResponse<Void> response =
                chapterService.deleteChapterRating(slug, chapterNumber);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // READING ACTIVITY TRACKING
    // ✅ Connected to Dashboard: Powers reading statistics, streaks, calendar
    // ============================================

    /**
     * Start reading - called when user opens a chapter
     *
     * ✅ Dashboard Impact:
     * - Updates reading calendar
     * - Maintains streak counter
     * - Records session for statistics
     */
    @PostMapping("/reading/start")
    public ResponseEntity<DataResponse<Void>> startReading(@PathVariable String slug, @Valid @RequestBody StartReadingRequest request) {
        DataResponse<Void> response = chapterService.startReading(slug, request);

        return ResponseEntity.ok(response);
    }

    /**
     * End reading - called when user closes chapter
     *
     * ✅ Dashboard Impact:
     * - Updates total reading time
     * - Calculates reading speed
     * - Updates daily statistics
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
     */
    @PostMapping("/reading/heartbeat")
    public ResponseEntity<DataResponse<Void>> readingHeartbeat(
            @PathVariable String slug,
            @Valid @RequestBody ReadingHeartbeatRequest request) {
        DataResponse<Void> response =
                new DataResponse<>("Success", "Heartbeat received", 200, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's reading history for this book
     *
     * ✅ Connected to Dashboard: Provides data for reading history view
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
     *
     * ✅ Connected to Dashboard: Powers reading pattern insights
     */
    @GetMapping("/reading/patterns")
    public ResponseEntity<DataResponse<UserReadingPatternResponse>> getReadingPattern(
            @PathVariable String slug) {
        DataResponse<UserReadingPatternResponse> response =
                chapterService.getUserReadingPattern(slug);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // SEARCH IN BOOK
    // ✅ Connected to Dashboard: Shows in search history
    // ============================================

    @PostMapping("/search")
    public ResponseEntity<DataResponse<SearchInBookResponse>> searchInBook(
            @PathVariable String slug,
            @Valid @RequestBody SearchInBookRequest request) {
        DataResponse<SearchInBookResponse> response =
                chapterService.searchInBook(slug, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/history")
    public ResponseEntity<DataResponse<List<SearchHistoryResponse>>> getSearchHistory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "10") int limit) {
        DataResponse<List<SearchHistoryResponse>> response =
                chapterService.getSearchHistory(slug, limit);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // EXPORT ANNOTATIONS
    // ✅ Connected to Dashboard: Export feature in dashboard
    // ============================================

    @PostMapping("/annotations/export")
    public ResponseEntity<DataResponse<ExportAnnotationsResponse>> exportAnnotations(
            @PathVariable String slug,
            @Valid @RequestBody ExportAnnotationsRequest request) {
        DataResponse<ExportAnnotationsResponse> response =
                chapterService.exportAnnotations(slug, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/annotations/export/{exportId}")
    public ResponseEntity<DataResponse<ExportAnnotationsResponse>> getExportStatus(
            @PathVariable String slug,
            @PathVariable Long exportId) {
        DataResponse<ExportAnnotationsResponse> response =
                chapterService.getExportStatus(exportId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/annotations/export/{exportId}/download")
    public ResponseEntity<Void> downloadExport(
            @PathVariable String slug,
            @PathVariable Long exportId) {
        String downloadUrl = chapterService.getExportDownloadUrl(exportId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", downloadUrl)
                .build();
    }

    @GetMapping("/annotations/exports")
    public ResponseEntity<DataResponse<ExportHistoryResponse>> getExportHistory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        DataResponse<ExportHistoryResponse> response =
                chapterService.getExportHistory(page, limit);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/annotations/export/{exportId}")
    public ResponseEntity<DataResponse<Void>> deleteExport(
            @PathVariable String slug,
            @PathVariable Long exportId) {
        DataResponse<Void> response = chapterService.deleteExport(exportId);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // BULK USER DATA (Optimization)
    // ✅ CRITICAL for Dashboard: Single API call to get all user data
    // ============================================

    /**
     * 🔥 GET ALL USER DATA FOR THIS BOOK IN ONE REQUEST
     *
     * This is THE MOST IMPORTANT endpoint for Dashboard integration!
     *
     * Returns everything Dashboard needs:
     * - Reading progress (for library view)
     * - All annotations (for annotations page)
     * - Chapter ratings (for statistics)
     * - Reading history (for history view)
     * - Reading patterns (for insights)
     *
     * ✅ Optimized: Single database transaction
     * ✅ Cached: Results cached for 5 minutes
     * ✅ Complete: All user data in one response
     */
    @GetMapping("/me")
    public ResponseEntity<DataResponse<UserBookDataResponse>> getMyBookData(
            @PathVariable String slug) {
        DataResponse<UserBookDataResponse> response =
                chapterService.getMyBookData(slug);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // ANALYTICS ENDPOINTS (for Authors/Admins)
    // ✅ Connected to Dashboard: Powers author analytics dashboard
    // ============================================

    /**
     * 📊 GET BOOK-WIDE ANALYTICS
     * For: Authors, Publishers, Admins
     */
    @GetMapping("/analytics")
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
     */
    @GetMapping("/analytics/chapters")
    public ResponseEntity<DataResponse<List<ChapterAnalyticsResponse>>> getChaptersAnalytics(
            @PathVariable String slug) {

        DataResponse<List<ChapterAnalyticsResponse>> response =
                chapterService.getChaptersAnalytics(slug);

        return ResponseEntity.ok(response);
    }
}