package com.naskah.demo.controller.book;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.book.BookReactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("api/books/{slug}")
@RequiredArgsConstructor
public class BookReactionController {

    private final BookReactionService reactionService;

    // RATING
    @PostMapping("/rating")
    public ResponseEntity<DataResponse<BookRatingResponse>> addOrUpdateBookRating(
            @PathVariable String slug,
            @Valid @RequestBody RatingRequest request) {
        DataResponse<BookRatingResponse> response = reactionService.addOrUpdateBookRating(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rating")
    public ResponseEntity<DataResponse<BookRatingStatsResponse>> getBookRatingStats(@PathVariable String slug) {
        DataResponse<BookRatingStatsResponse> response = reactionService.getBookRatingStats(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rating/me")
    public ResponseEntity<DataResponse<BookRatingResponse>> getMyBookRating(@PathVariable String slug) {
        DataResponse<BookRatingResponse> response = reactionService.getMyBookRating(slug);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rating")
    public ResponseEntity<DataResponse<Void>> deleteBookRating(@PathVariable String slug) {
        DataResponse<Void> response = reactionService.deleteBookRating(slug);
        return ResponseEntity.ok(response);
    }

    // REVIEW
    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<BookReviewResponse>> getBookReviews(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "helpful") String sortBy) {
        DatatableResponse<BookReviewResponse> response = reactionService.getBookReviews(slug, page, limit, sortBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews/me")
    public ResponseEntity<DataResponse<BookReviewResponse>> getMyBookReview(@PathVariable String slug) {
        DataResponse<BookReviewResponse> response = reactionService.getMyBookReview(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reviews")
    public ResponseEntity<DataResponse<BookReviewResponse>> createBookReview(
            @PathVariable String slug,
            @Valid @RequestBody BookReviewRequest request) {
        DataResponse<BookReviewResponse> response = reactionService.createBookReview(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/reviews")
    public ResponseEntity<DataResponse<BookReviewResponse>> updateBookReview(
            @PathVariable String slug,
            @Valid @RequestBody BookReviewRequest request) {
        DataResponse<BookReviewResponse> response = reactionService.updateBookReview(slug, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews")
    public ResponseEntity<DataResponse<Void>> deleteBookReview(@PathVariable String slug) {
        DataResponse<Void> response = reactionService.deleteBookReview(slug);
        return ResponseEntity.ok(response);
    }

    // REPLY
    @PostMapping("/reviews/{reviewId}/replies")
    public ResponseEntity<DataResponse<BookReviewReplyResponse>> addReplyToBookReview(
            @PathVariable String slug,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReplyRequest request) {
        DataResponse<BookReviewReplyResponse> response = reactionService.addReplyToBookReview(slug, reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/reviews/replies/{replyId}")
    public ResponseEntity<DataResponse<BookReviewReplyResponse>> updateBookReviewReply(
            @PathVariable String slug,
            @PathVariable Long replyId,
            @Valid @RequestBody ReplyRequest request) {
        DataResponse<BookReviewReplyResponse> response = reactionService.updateBookReviewReply(slug, replyId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews/replies/{replyId}")
    public ResponseEntity<DataResponse<Void>> deleteBookReviewReply(
            @PathVariable String slug,
            @PathVariable Long replyId) {
        DataResponse<Void> response = reactionService.deleteBookReviewReply(slug, replyId);
        return ResponseEntity.ok(response);
    }

    // FEEDBACK
    @PostMapping("/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> addOrUpdateBookReviewFeedback(
            @PathVariable String slug,
            @PathVariable Long reviewId,
            @Valid @RequestBody FeedbackRequest request) {
        DataResponse<Void> response = reactionService.addOrUpdateBookReviewFeedback(slug, reviewId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> deleteBookReviewFeedback(
            @PathVariable String slug,
            @PathVariable Long reviewId) {
        DataResponse<Void> response = reactionService.deleteBookReviewFeedback(slug, reviewId);
        return ResponseEntity.ok(response);
    }
}
