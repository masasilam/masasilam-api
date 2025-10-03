package com.naskah.demo.controller;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // ============ BOOK CRUD ENDPOINTS ============
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<BookResponse>> createBook(@Valid @ModelAttribute BookRequest request) {
        DataResponse<BookResponse> response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<DataResponse<BookResponse>> getBookDetail(@PathVariable String slug) {
        DataResponse<BookResponse> response = bookService.getBookDetailBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<DatatableResponse<BookResponse>> getBooksPaginated(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "updateAt", required = false) String sortField,
            @RequestParam(defaultValue = "DESC", required = false) String sortOrder,
            @RequestParam(required = false) String searchTitle,
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Long subGenreId) {

        DatatableResponse<BookResponse> response = bookService.getPaginatedBooks(
                page, limit, sortField, sortOrder, searchTitle, seriesId, genreId, subGenreId);
        return ResponseEntity.ok(response);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<Book>> update(
            @RequestParam Long id,
            @RequestPart("ebook") @Valid Book book,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        DataResponse<Book> response = bookService.update(id, book, file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DefaultResponse> delete(@PathVariable Long id) throws IOException {
        DefaultResponse response = bookService.delete(id);
        return ResponseEntity.ok(response);
    }

    // ============ READING & PROGRESS ENDPOINTS ============
    @GetMapping("/{slug}/read")
    public ResponseEntity<DataResponse<ReadingResponse>> startReading(@PathVariable String slug) {
        DataResponse<ReadingResponse> response = bookService.startReading(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/progress")
    public ResponseEntity<DataResponse<ReadingProgressResponse>> saveProgress(
            @PathVariable String slug,
            @Valid @RequestBody ProgressRequest request) {
        DataResponse<ReadingProgressResponse> response = bookService.saveReadingProgress(slug, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}/progress")
    public ResponseEntity<DataResponse<ReadingProgressResponse>> getProgress(@PathVariable String slug) {
        DataResponse<ReadingProgressResponse> response = bookService.getReadingProgress(slug);
        return ResponseEntity.ok(response);
    }

    // ============ BOOKMARKS ENDPOINTS ============
    @PostMapping("/{slug}/bookmarks")
    public ResponseEntity<DataResponse<BookmarkResponse>> addBookmark(
            @PathVariable String slug,
            @Valid @RequestBody BookmarkRequest request) {
        DataResponse<BookmarkResponse> response = bookService.addBookmark(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}/bookmarks")
    public ResponseEntity<DataResponse<List<BookmarkResponse>>> getBookmarks(@PathVariable String slug) {
        DataResponse<List<BookmarkResponse>> response = bookService.getBookmarks(slug);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{slug}/bookmarks/{bookmarkId}")
    public ResponseEntity<DataResponse<BookmarkResponse>> updateBookmark(
            @PathVariable String slug,
            @PathVariable Long bookmarkId,
            @Valid @RequestBody BookmarkRequest request) {
        DataResponse<BookmarkResponse> response = bookService.updateBookmark(slug, bookmarkId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}/bookmarks/{bookmarkId}")
    public ResponseEntity<DefaultResponse> deleteBookmark(
            @PathVariable String slug,
            @PathVariable Long bookmarkId) {
        DefaultResponse response = bookService.deleteBookmark(slug, bookmarkId);
        return ResponseEntity.ok(response);
    }

    // ============ HIGHLIGHTS ENDPOINTS ============
    @PostMapping("/{slug}/highlights")
    public ResponseEntity<DataResponse<HighlightResponse>> addHighlight(
            @PathVariable String slug,
            @Valid @RequestBody HighlightRequest request) {
        DataResponse<HighlightResponse> response = bookService.addHighlight(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}/highlights")
    public ResponseEntity<DataResponse<List<HighlightResponse>>> getHighlights(@PathVariable String slug) {
        DataResponse<List<HighlightResponse>> response = bookService.getHighlights(slug);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{slug}/highlights/{highlightId}")
    public ResponseEntity<DataResponse<HighlightResponse>> updateHighlight(
            @PathVariable String slug,
            @PathVariable Long highlightId,
            @Valid @RequestBody HighlightRequest request) {
        DataResponse<HighlightResponse> response = bookService.updateHighlight(slug, highlightId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}/highlights/{highlightId}")
    public ResponseEntity<DefaultResponse> deleteHighlight(
            @PathVariable String slug,
            @PathVariable Long highlightId) {
        DefaultResponse response = bookService.deleteHighlight(slug, highlightId);
        return ResponseEntity.ok(response);
    }

    // ============ NOTES ENDPOINTS ============
    @PostMapping("/{slug}/notes")
    public ResponseEntity<DataResponse<NoteResponse>> addNote(
            @PathVariable String slug,
            @Valid @RequestBody NoteRequest request) {
        DataResponse<NoteResponse> response = bookService.addNote(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}/notes")
    public ResponseEntity<DataResponse<List<NoteResponse>>> getNotes(@PathVariable String slug) {
        DataResponse<List<NoteResponse>> response = bookService.getNotes(slug);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{slug}/notes/{noteId}")
    public ResponseEntity<DataResponse<NoteResponse>> updateNote(
            @PathVariable String slug,
            @PathVariable Long noteId,
            @Valid @RequestBody NoteRequest request) {
        DataResponse<NoteResponse> response = bookService.updateNote(slug, noteId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{slug}/notes/{noteId}")
    public ResponseEntity<DefaultResponse> deleteNote(
            @PathVariable String slug,
            @PathVariable Long noteId) {
        DefaultResponse response = bookService.deleteNote(slug, noteId);
        return ResponseEntity.ok(response);
    }

    // ============ RATING ENDPOINTS ============

    /**
     * POST /api/books/{slug}/rating
     * Add or update rating for a book
     */
    @PostMapping("/{slug}/rating")
    public ResponseEntity<DataResponse<ReactionResponse>> addOrUpdateRating(
            @PathVariable String slug,
            @RequestBody @Valid RatingRequest request) {

        DataResponse<ReactionResponse> response = bookService.addOrUpdateRating(slug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/books/{slug}/rating
     * Delete user's rating for a book
     */
    @DeleteMapping("/{slug}/rating")
    public ResponseEntity<DataResponse<Void>> deleteRating(@PathVariable String slug) {
        DataResponse<Void> response = bookService.deleteRating(slug);
        return ResponseEntity.ok(response);
    }

    // ============ REVIEW ENDPOINTS ============

    /**
     * GET /api/books/{slug}/reviews
     * Get all reviews for a book with pagination
     */
    @GetMapping("/{slug}/reviews")
    public ResponseEntity<DataResponse<List<ReactionResponse>>> getReviews(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        DataResponse<List<ReactionResponse>> response = bookService.getReviews(slug, page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/books/{slug}/reviews
     * Add a new review for a book
     */
    @PostMapping("/{slug}/reviews")
    public ResponseEntity<DataResponse<ReactionResponse>> addReview(
            @PathVariable String slug,
            @RequestBody @Valid ReviewRequest request) {

        DataResponse<ReactionResponse> response = bookService.addReview(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/books/{slug}/reviews
     * Update user's existing review
     */
    @PutMapping("/{slug}/reviews")
    public ResponseEntity<DataResponse<ReactionResponse>> updateReview(
            @PathVariable String slug,
            @RequestBody @Valid ReviewRequest request) {

        DataResponse<ReactionResponse> response = bookService.updateReview(slug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/books/{slug}/reviews
     * Delete user's review for a book
     */
    @DeleteMapping("/{slug}/reviews")
    public ResponseEntity<DataResponse<Void>> deleteReview(@PathVariable String slug) {
        DataResponse<Void> response = bookService.deleteReview(slug);
        return ResponseEntity.ok(response);
    }

    // ============ REPLY ENDPOINTS ============

    /**
     * POST /api/books/{slug}/reviews/{parentId}/replies
     * Add a reply to a review or comment
     */
    @PostMapping("/{slug}/reviews/{parentId}/replies")
    public ResponseEntity<DataResponse<ReactionResponse>> addReply(
            @PathVariable String slug,
            @PathVariable Long parentId,
            @RequestBody @Valid ReplyRequest request) {

        DataResponse<ReactionResponse> response = bookService.addReply(slug, parentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/books/{slug}/replies/{replyId}
     * Delete a reply
     */
    @DeleteMapping("/{slug}/replies/{replyId}")
    public ResponseEntity<DataResponse<Void>> deleteReply(
            @PathVariable String slug,
            @PathVariable Long replyId) {

        DataResponse<Void> response = bookService.deleteReply(slug, replyId);
        return ResponseEntity.ok(response);
    }

    // ============ FEEDBACK ENDPOINTS ============

    /**
     * POST /api/books/{slug}/reviews/{reviewId}/feedback
     * Add or update feedback (HELPFUL/NOT_HELPFUL) on a review
     */
    @PostMapping("/{slug}/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<ReactionResponse>> addOrUpdateFeedback(
            @PathVariable String slug,
            @PathVariable Long reviewId,
            @RequestBody @Valid FeedbackRequest request) {

        DataResponse<ReactionResponse> response = bookService.addOrUpdateFeedback(slug, reviewId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/books/{slug}/reviews/{reviewId}/feedback
     * Delete user's feedback on a review
     */
    @DeleteMapping("/{slug}/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> deleteFeedback(
            @PathVariable String slug,
            @PathVariable Long reviewId) {

        DataResponse<Void> response = bookService.deleteFeedback(slug, reviewId);
        return ResponseEntity.ok(response);
    }

    // ============ UTILITY ENDPOINTS ============
    @GetMapping("/{slug}/download")
    public ResponseEntity<byte[]> downloadBook(@PathVariable String slug) {
        return bookService.downloadBookAsBytes(slug);
    }

    @GetMapping("/{slug}/search")
    public ResponseEntity<DataResponse<SearchResultResponse>> searchInBook(
            @PathVariable String slug,
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        DataResponse<SearchResultResponse> response = bookService.searchInBook(slug, query, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/translate")
    public ResponseEntity<DataResponse<TranslationResponse>> translateText(
            @PathVariable String slug,
            @Valid @RequestBody TranslationRequest request) {
        DataResponse<TranslationResponse> response = bookService.translateText(slug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/translate-highlight")
    public ResponseEntity<DataResponse<TranslatedHighlightResponse>> translateHighlight(
            @PathVariable String slug,
            @Valid @RequestBody TranslateHighlightRequest request) {
        DataResponse<TranslatedHighlightResponse> response = bookService.translateHighlight(slug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/tts")
    public ResponseEntity<DataResponse<TTSResponse>> generateTextToSpeech(
            @PathVariable String slug,
            @Valid @RequestBody TTSRequest request) {
        DataResponse<TTSResponse> response = bookService.generateTextToSpeech(slug, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/sync-audio")
    public ResponseEntity<DataResponse<AudioSyncResponse>> syncAudioWithText(
            @PathVariable String slug,
            @Valid @RequestBody AudioSyncRequest request) {
        DataResponse<AudioSyncResponse> response = bookService.syncAudioWithText(slug, request);
        return ResponseEntity.ok(response);
    }
}