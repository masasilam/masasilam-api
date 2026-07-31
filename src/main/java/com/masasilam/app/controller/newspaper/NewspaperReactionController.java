package com.masasilam.app.controller.newspaper;

import com.masasilam.app.model.dto.newspaper.*;
import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.service.newspaper.NewspaperReactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/newspapers/{sourceSlug}/{date}/{articleSlug}")
@RequiredArgsConstructor
public class NewspaperReactionController {
    private final NewspaperReactionService reactionService;

    @PostMapping("/rating")
    public ResponseEntity<DataResponse<ArticleRatingResponse>> rateArticle(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @Valid @RequestBody RatingRequest request) {
        DataResponse<ArticleRatingResponse> response = reactionService.addOrUpdateArticleRating(sourceSlug, date, articleSlug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rating")
    public ResponseEntity<DataResponse<ArticleRatingStatsResponse>> getArticleRatingStats(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug) {
        DataResponse<ArticleRatingStatsResponse> response = reactionService.getArticleRatingStats(sourceSlug, date, articleSlug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rating/me")
    public ResponseEntity<DataResponse<ArticleRatingResponse>> getMyArticleRating(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug) {
        DataResponse<ArticleRatingResponse> response = reactionService.getMyArticleRating(sourceSlug, date, articleSlug);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rating")
    public ResponseEntity<DataResponse<Void>> deleteArticleRating(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug) {
        DataResponse<Void> response = reactionService.deleteArticleRating(sourceSlug, date, articleSlug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<ArticleReviewResponse>> getArticleReviews(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue = "10") @Min(1) int limit, @RequestParam(defaultValue = "helpful") String sortBy) {
        DatatableResponse<ArticleReviewResponse> response = reactionService.getArticleReviews(sourceSlug, date, articleSlug, page, limit, sortBy);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reviews")
    public ResponseEntity<DataResponse<ArticleReviewResponse>> createArticleReview(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @Valid @RequestBody ArticleReviewRequest request) {
        DataResponse<ArticleReviewResponse> response = reactionService.createArticleReview(sourceSlug, date, articleSlug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/reviews/me")
    public ResponseEntity<DataResponse<ArticleReviewResponse>> updateMyArticleReview(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @Valid @RequestBody ArticleReviewRequest request) {
        DataResponse<ArticleReviewResponse> response = reactionService.updateMyArticleReview(sourceSlug, date, articleSlug, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews/me")
    public ResponseEntity<DataResponse<Void>> deleteMyArticleReview(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug) {
        DataResponse<Void> response = reactionService.deleteMyArticleReview(sourceSlug, date, articleSlug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reviews/{reviewId}/replies")
    public ResponseEntity<DataResponse<ArticleReviewReplyResponse>> replyToReview(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @PathVariable Long reviewId, @Valid @RequestBody ReplyRequest request) {
        DataResponse<ArticleReviewReplyResponse> response = reactionService.addReplyToReview(sourceSlug, date, articleSlug, reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/reviews/replies/{replyId}")
    public ResponseEntity<DataResponse<ArticleReviewReplyResponse>> updateReply(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @PathVariable Long replyId, @Valid @RequestBody ReplyRequest request) {
        DataResponse<ArticleReviewReplyResponse> response = reactionService.updateReply(replyId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews/replies/{replyId}")
    public ResponseEntity<DataResponse<Void>> deleteReply(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @PathVariable Long replyId) {
        DataResponse<Void> response = reactionService.deleteReply(replyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> addReviewFeedback(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @PathVariable Long reviewId, @Valid @RequestBody FeedbackRequest request) {
        DataResponse<Void> response = reactionService.addOrUpdateReviewFeedback(reviewId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> deleteReviewFeedback(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @PathVariable Long reviewId) {
        DataResponse<Void> response = reactionService.deleteReviewFeedback(reviewId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    public ResponseEntity<DataResponse<SavedArticleResponse>> saveArticle(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @Valid @RequestBody SaveArticleRequest request) {
        DataResponse<SavedArticleResponse> response = reactionService.saveArticle(sourceSlug, date, articleSlug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/save")
    public ResponseEntity<DataResponse<SavedArticleResponse>> checkArticleSaved(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug) {
        DataResponse<SavedArticleResponse> response = reactionService.checkArticleSaved(sourceSlug, date, articleSlug);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/save")
    public ResponseEntity<DataResponse<Void>> unsaveArticle(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug) {
        DataResponse<Void> response = reactionService.unsaveArticle(sourceSlug, date, articleSlug);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/share")
    public ResponseEntity<DataResponse<Void>> trackArticleShare(@PathVariable String sourceSlug, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable String articleSlug, @Valid @RequestBody ShareArticleRequest request) {
        DataResponse<Void> response = reactionService.trackArticleShare(sourceSlug, date, articleSlug, request);
        return ResponseEntity.ok(response);
    }
}