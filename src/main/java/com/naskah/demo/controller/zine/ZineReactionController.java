package com.naskah.demo.controller.zine;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.zine.ZineReactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/zines/{slug}")
@RequiredArgsConstructor
public class ZineReactionController {

    private final ZineReactionService reactionService;

    // ─── RATING ───────────────────────────────────────────────────────────────

    @PostMapping("/rating")
    public ResponseEntity<DataResponse<ZineRatingResponse>> addOrUpdateRating(
            @PathVariable String slug, @Valid @RequestBody RatingRequest request) {
        DataResponse<ZineRatingResponse> response = reactionService.addOrUpdateZineRating(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rating")
    public ResponseEntity<DataResponse<ZineRatingStatsResponse>> getRatingStats(
            @PathVariable String slug) {
        DataResponse<ZineRatingStatsResponse> response = reactionService.getZineRatingStats(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rating/me")
    public ResponseEntity<DataResponse<ZineRatingResponse>> getMyRating(
            @PathVariable String slug) {
        DataResponse<ZineRatingResponse> response = reactionService.getMyZineRating(slug);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rating")
    public ResponseEntity<DataResponse<Void>> deleteRating(@PathVariable String slug) {
        DataResponse<Void> response = reactionService.deleteZineRating(slug);
        return ResponseEntity.ok(response);
    }

    // ─── REVIEW ───────────────────────────────────────────────────────────────

    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<ZineReviewResponse>> getReviews(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "helpful") String sortBy) {
        DatatableResponse<ZineReviewResponse> response =
                reactionService.getZineReviews(slug, page, limit, sortBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews/me")
    public ResponseEntity<DataResponse<ZineReviewResponse>> getMyReview(@PathVariable String slug) {
        DataResponse<ZineReviewResponse> response = reactionService.getMyZineReview(slug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reviews")
    public ResponseEntity<DataResponse<ZineReviewResponse>> createReview(
            @PathVariable String slug, @Valid @RequestBody BookReviewRequest request) {
        DataResponse<ZineReviewResponse> response = reactionService.createZineReview(slug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/reviews")
    public ResponseEntity<DataResponse<ZineReviewResponse>> updateReview(
            @PathVariable String slug, @Valid @RequestBody BookReviewRequest request) {
        DataResponse<ZineReviewResponse> response = reactionService.updateZineReview(slug, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews")
    public ResponseEntity<DataResponse<Void>> deleteReview(@PathVariable String slug) {
        DataResponse<Void> response = reactionService.deleteZineReview(slug);
        return ResponseEntity.ok(response);
    }

    // ─── REPLY ────────────────────────────────────────────────────────────────

    @PostMapping("/reviews/{reviewId}/replies")
    public ResponseEntity<DataResponse<ZineReviewReplyResponse>> addReply(
            @PathVariable String slug, @PathVariable Long reviewId,
            @Valid @RequestBody ReplyRequest request) {
        DataResponse<ZineReviewReplyResponse> response =
                reactionService.addReplyToZineReview(slug, reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/reviews/replies/{replyId}")
    public ResponseEntity<DataResponse<ZineReviewReplyResponse>> updateReply(
            @PathVariable String slug, @PathVariable Long replyId,
            @Valid @RequestBody ReplyRequest request) {
        DataResponse<ZineReviewReplyResponse> response =
                reactionService.updateZineReviewReply(slug, replyId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews/replies/{replyId}")
    public ResponseEntity<DataResponse<Void>> deleteReply(
            @PathVariable String slug, @PathVariable Long replyId) {
        DataResponse<Void> response = reactionService.deleteZineReviewReply(slug, replyId);
        return ResponseEntity.ok(response);
    }

    // ─── FEEDBACK ─────────────────────────────────────────────────────────────

    @PostMapping("/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> addOrUpdateFeedback(
            @PathVariable String slug, @PathVariable Long reviewId,
            @Valid @RequestBody FeedbackRequest request) {
        DataResponse<Void> response =
                reactionService.addOrUpdateZineReviewFeedback(slug, reviewId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> deleteFeedback(
            @PathVariable String slug, @PathVariable Long reviewId) {
        DataResponse<Void> response = reactionService.deleteZineReviewFeedback(slug, reviewId);
        return ResponseEntity.ok(response);
    }
}