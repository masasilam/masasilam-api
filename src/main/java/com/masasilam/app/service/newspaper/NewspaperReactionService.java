package com.masasilam.app.service.newspaper;

import com.masasilam.app.model.dto.newspaper.*;
import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;

import java.time.LocalDate;

public interface NewspaperReactionService {
    DataResponse<ArticleRatingResponse> addOrUpdateArticleRating(String sourceSlug, LocalDate date, String articleSlug, RatingRequest request);
    DataResponse<ArticleRatingStatsResponse> getArticleRatingStats(String sourceSlug, LocalDate date, String articleSlug);
    DataResponse<ArticleRatingResponse> getMyArticleRating(String sourceSlug, LocalDate date, String articleSlug);
    DataResponse<Void> deleteArticleRating(String sourceSlug, LocalDate date, String articleSlug);
    DatatableResponse<ArticleReviewResponse> getArticleReviews(String sourceSlug, LocalDate date, String articleSlug, int page, int limit, String sortBy);
    DataResponse<ArticleReviewResponse> createArticleReview(String sourceSlug, LocalDate date, String articleSlug, ArticleReviewRequest request);
    DataResponse<ArticleReviewResponse> updateMyArticleReview(String sourceSlug, LocalDate date, String articleSlug, ArticleReviewRequest request);
    DataResponse<Void> deleteMyArticleReview(String sourceSlug, LocalDate date, String articleSlug);
    DataResponse<ArticleReviewReplyResponse> addReplyToReview(String sourceSlug, LocalDate date, String articleSlug, Long reviewId, ReplyRequest request);
    DataResponse<ArticleReviewReplyResponse> updateReply(Long replyId, ReplyRequest request);
    DataResponse<Void> deleteReply(Long replyId);
    DataResponse<Void> addOrUpdateReviewFeedback(Long reviewId, FeedbackRequest request);
    DataResponse<Void> deleteReviewFeedback(Long reviewId);
    DataResponse<SavedArticleResponse> saveArticle(String sourceSlug, LocalDate date, String articleSlug, SaveArticleRequest request);
    DataResponse<SavedArticleResponse> checkArticleSaved(String sourceSlug, LocalDate date, String articleSlug);
    DataResponse<Void> unsaveArticle(String sourceSlug, LocalDate date, String articleSlug);
    DataResponse<Void> trackArticleShare(String sourceSlug, LocalDate date, String articleSlug, ShareArticleRequest request);
}