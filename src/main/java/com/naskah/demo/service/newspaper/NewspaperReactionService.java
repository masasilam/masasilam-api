package com.naskah.demo.service.newspaper;

import com.naskah.demo.model.dto.newspaper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;

import java.time.LocalDate;

/**
 * NewspaperReactionService - Handle user interactions with articles
 */
public interface NewspaperReactionService {

    // ============================================
    // ARTICLE RATING
    // ============================================

    /**
     * Add or update article rating
     */
    DataResponse<ArticleRatingResponse> addOrUpdateArticleRating(
            String categorySlug,
            LocalDate date,
            String articleSlug,
            RatingRequest request);

    /**
     * Get article rating statistics
     */
    DataResponse<ArticleRatingStatsResponse> getArticleRatingStats(
            String categorySlug,
            LocalDate date,
            String articleSlug);

    /**
     * Get my rating for this article
     */
    DataResponse<ArticleRatingResponse> getMyArticleRating(
            String categorySlug,
            LocalDate date,
            String articleSlug);

    /**
     * Delete my article rating
     */
    DataResponse<Void> deleteArticleRating(
            String categorySlug,
            LocalDate date,
            String articleSlug);

    // ============================================
    // ARTICLE REVIEWS
    // ============================================

    /**
     * Get all reviews for an article
     */
    DatatableResponse<ArticleReviewResponse> getArticleReviews(
            String categorySlug,
            LocalDate date,
            String articleSlug,
            int page,
            int limit,
            String sortBy);

    /**
     * Create a review
     */
    DataResponse<ArticleReviewResponse> createArticleReview(
            String categorySlug,
            LocalDate date,
            String articleSlug,
            ArticleReviewRequest request);

    /**
     * Update my review
     */
    DataResponse<ArticleReviewResponse> updateMyArticleReview(
            String categorySlug,
            LocalDate date,
            String articleSlug,
            ArticleReviewRequest request);

    /**
     * Delete my review
     */
    DataResponse<Void> deleteMyArticleReview(
            String categorySlug,
            LocalDate date,
            String articleSlug);

    // ============================================
    // REVIEW REPLIES
    // ============================================

    /**
     * Add reply to a review
     */
    DataResponse<ArticleReviewReplyResponse> addReplyToReview(
            String categorySlug,
            LocalDate date,
            String articleSlug,
            Long reviewId,
            ReplyRequest request);

    /**
     * Update a reply
     */
    DataResponse<ArticleReviewReplyResponse> updateReply(
            Long replyId,
            ReplyRequest request);

    /**
     * Delete a reply
     */
    DataResponse<Void> deleteReply(Long replyId);

    // ============================================
    // REVIEW FEEDBACK
    // ============================================

    /**
     * Add or update review feedback (helpful/not helpful)
     */
    DataResponse<Void> addOrUpdateReviewFeedback(
            Long reviewId,
            FeedbackRequest request);

    /**
     * Delete review feedback
     */
    DataResponse<Void> deleteReviewFeedback(Long reviewId);

    // ============================================
    // SAVE ARTICLE
    // ============================================

    /**
     * Save article to my collection
     */
    DataResponse<SavedArticleResponse> saveArticle(
            String categorySlug,
            LocalDate date,
            String articleSlug,
            SaveArticleRequest request);

    /**
     * Check if article is saved
     */
    DataResponse<SavedArticleResponse> checkArticleSaved(
            String categorySlug,
            LocalDate date,
            String articleSlug);

    /**
     * Unsave article
     */
    DataResponse<Void> unsaveArticle(
            String categorySlug,
            LocalDate date,
            String articleSlug);

    // ============================================
    // SHARE TRACKING
    // ============================================

    /**
     * Track article share
     */
    DataResponse<Void> trackArticleShare(
            String categorySlug,
            LocalDate date,
            String articleSlug,
            ShareArticleRequest request);
}