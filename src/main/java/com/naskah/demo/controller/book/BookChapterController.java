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

    // ============ CHAPTER STATISTICS ============

    /**
     * Get chapter reading statistics
     * Shows: completion rate, average time, most highlighted passages
     */
    @GetMapping("/{chapterNumber}/stats")
    public ResponseEntity<DataResponse<ChapterStatsResponse>> getChapterStats(
            @PathVariable String slug,
            @PathVariable Integer chapterNumber) {
        DataResponse<ChapterStatsResponse> response = chapterService.getChapterStats(slug, chapterNumber);
        return ResponseEntity.ok(response);
    }
}