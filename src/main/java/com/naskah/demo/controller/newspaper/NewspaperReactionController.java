package com.naskah.demo.controller.newspaper;

import com.naskah.demo.model.dto.newspaper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.service.newspaper.NewspaperReactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * NewspaperReactionController - Handle user interactions with articles
 * <p>
 * Features:
 * - Rating articles
 * - Reviewing articles
 * - Commenting on articles
 * - Saving articles (bookmark)
 * - Sharing articles
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/newspapers/{categorySlug}/{date}/{articleSlug}")
@RequiredArgsConstructor
public class NewspaperReactionController {

    private final NewspaperReactionService reactionService;

    // ============================================
    // ARTICLE RATING
    // ============================================

    /**
     * Rate an article (1-5 stars)
     * POST /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/rating
     */
    @PostMapping("/rating")
    public ResponseEntity<DataResponse<ArticleRatingResponse>> rateArticle(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @Valid @RequestBody RatingRequest request) {

        DataResponse<ArticleRatingResponse> response =
                reactionService.addOrUpdateArticleRating(categorySlug, date, articleSlug, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get article rating stats
     * GET /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/rating
     */
    @GetMapping("/rating")
    public ResponseEntity<DataResponse<ArticleRatingStatsResponse>> getArticleRatingStats(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug) {

        DataResponse<ArticleRatingStatsResponse> response =
                reactionService.getArticleRatingStats(categorySlug, date, articleSlug);

        return ResponseEntity.ok(response);
    }

    /**
     * Get my rating for this article
     * GET /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/rating/me
     */
    @GetMapping("/rating/me")
    public ResponseEntity<DataResponse<ArticleRatingResponse>> getMyArticleRating(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug) {

        DataResponse<ArticleRatingResponse> response =
                reactionService.getMyArticleRating(categorySlug, date, articleSlug);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete my rating
     * DELETE /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/rating
     */
    @DeleteMapping("/rating")
    public ResponseEntity<DataResponse<Void>> deleteArticleRating(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug) {

        DataResponse<Void> response =
                reactionService.deleteArticleRating(categorySlug, date, articleSlug);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // ARTICLE REVIEWS
    // ============================================

    /**
     * Get all reviews for an article
     * GET /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/reviews
     */
    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<ArticleReviewResponse>> getArticleReviews(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "helpful") String sortBy) {

        DatatableResponse<ArticleReviewResponse> response =
                reactionService.getArticleReviews(categorySlug, date, articleSlug, page, limit, sortBy);

        return ResponseEntity.ok(response);
    }

    /**
     * Create a review
     * POST /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/reviews
     */
    @PostMapping("/reviews")
    public ResponseEntity<DataResponse<ArticleReviewResponse>> createArticleReview(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @Valid @RequestBody ArticleReviewRequest request) {

        DataResponse<ArticleReviewResponse> response =
                reactionService.createArticleReview(categorySlug, date, articleSlug, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update my review
     * PUT /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/reviews/me
     */
    @PutMapping("/reviews/me")
    public ResponseEntity<DataResponse<ArticleReviewResponse>> updateMyArticleReview(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @Valid @RequestBody ArticleReviewRequest request) {

        DataResponse<ArticleReviewResponse> response =
                reactionService.updateMyArticleReview(categorySlug, date, articleSlug, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete my review
     * DELETE /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/reviews/me
     */
    @DeleteMapping("/reviews/me")
    public ResponseEntity<DataResponse<Void>> deleteMyArticleReview(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug) {

        DataResponse<Void> response = reactionService.deleteMyArticleReview(categorySlug, date, articleSlug);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // REVIEW REPLIES
    // ============================================

    /**
     * Reply to a review
     * POST /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/reviews/{reviewId}/replies
     */
    @PostMapping("/reviews/{reviewId}/replies")
    public ResponseEntity<DataResponse<ArticleReviewReplyResponse>> replyToReview(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReplyRequest request) {

        DataResponse<ArticleReviewReplyResponse> response = reactionService.addReplyToReview(categorySlug, date, articleSlug, reviewId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update a reply
     * PUT /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/reviews/replies/{replyId}
     */
    @PutMapping("/reviews/replies/{replyId}")
    public ResponseEntity<DataResponse<ArticleReviewReplyResponse>> updateReply(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @PathVariable Long replyId,
            @Valid @RequestBody ReplyRequest request) {

        DataResponse<ArticleReviewReplyResponse> response = reactionService.updateReply(replyId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a reply
     * DELETE /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/reviews/replies/{replyId}
     */
    @DeleteMapping("/reviews/replies/{replyId}")
    public ResponseEntity<DataResponse<Void>> deleteReply(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @PathVariable Long replyId) {

        DataResponse<Void> response = reactionService.deleteReply(replyId);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // REVIEW FEEDBACK (Helpful/Not Helpful)
    // ============================================

    /**
     * Mark review as helpful/not helpful
     * POST /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/reviews/{reviewId}/feedback
     */
    @PostMapping("/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> addReviewFeedback(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @PathVariable Long reviewId,
            @Valid @RequestBody FeedbackRequest request) {

        DataResponse<Void> response = reactionService.addOrUpdateReviewFeedback(reviewId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove feedback from review
     * DELETE /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/reviews/{reviewId}/feedback
     */
    @DeleteMapping("/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> deleteReviewFeedback(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @PathVariable Long reviewId) {

        DataResponse<Void> response = reactionService.deleteReviewFeedback(reviewId);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // SAVE ARTICLE (Bookmark)
    // ============================================

    /**
     * Save article to my collection
     * POST /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/save
     */
    @PostMapping("/save")
    public ResponseEntity<DataResponse<SavedArticleResponse>> saveArticle(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @Valid @RequestBody SaveArticleRequest request) {

        DataResponse<SavedArticleResponse> response = reactionService.saveArticle(categorySlug, date, articleSlug, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Check if article is saved
     * GET /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/save
     */
    @GetMapping("/save")
    public ResponseEntity<DataResponse<SavedArticleResponse>> checkArticleSaved(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug) {

        DataResponse<SavedArticleResponse> response = reactionService.checkArticleSaved(categorySlug, date, articleSlug);

        return ResponseEntity.ok(response);
    }

    /**
     * Unsave article
     * DELETE /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/save
     */
    @DeleteMapping("/save")
    public ResponseEntity<DataResponse<Void>> unsaveArticle(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug) {

        DataResponse<Void> response = reactionService.unsaveArticle(categorySlug, date, articleSlug);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // SHARE TRACKING
    // ============================================

    /**
     * Track article share
     * POST /api/newspapers/olahraga/1945-01-01/awal-berdirinya-persebaya/share
     */
    @PostMapping("/share")
    public ResponseEntity<DataResponse<Void>> trackArticleShare(
            @PathVariable String categorySlug,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String articleSlug,
            @Valid @RequestBody ShareArticleRequest request) {

        DataResponse<Void> response = reactionService.trackArticleShare(categorySlug, date, articleSlug, request);

        return ResponseEntity.ok(response);
    }
}