package com.masasilam.app.service.film;

import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;

import java.util.List;

public interface FilmReactionService {
    DataResponse<FilmRatingResponse> addOrUpdateRating(String slug, RatingRequest request);
    DataResponse<FilmRatingStatsResponse> getRatingStats(String slug);
    DataResponse<FilmRatingResponse> getMyRating(String slug);
    DataResponse<Void> deleteRating(String slug);
    DatatableResponse<FilmReviewResponse> getReviews(String slug, int page, int limit, String sortBy);
    DataResponse<FilmReviewResponse> getMyReview(String slug);
    DataResponse<FilmReviewResponse> createReview(String slug, FilmReviewRequest request);
    DataResponse<FilmReviewResponse> updateReview(String slug, FilmReviewRequest request);
    DataResponse<Void> deleteReview(String slug);
    DataResponse<FilmReviewReplyResponse> addReply(String slug, Long reviewId, ReplyRequest request);
    DataResponse<FilmReviewReplyResponse> updateReply(String slug, Long replyId, ReplyRequest request);
    DataResponse<Void> deleteReply(String slug, Long replyId);
    DataResponse<Void> addOrUpdateFeedback(String slug, Long reviewId, FeedbackRequest request);
    DataResponse<Void> deleteFeedback(String slug, Long reviewId);
    DataResponse<Void> addToWatchlist(String slug);
    DataResponse<Void> removeFromWatchlist(String slug);
    DataResponse<Boolean> isInWatchlist(String slug);
    DatatableResponse<FilmWatchlistResponse> getMyWatchlist(int page, int limit);
    DataResponse<Void> updateWatchProgress(String slug, WatchProgressRequest request);
    DataResponse<WatchProgressResponse> getMyProgress(String slug);
    DataResponse<List<VideoSourceResponse>> getVideoSources(String slug);
    DataResponse<VideoSourceResponse> addVideoSource(String slug, AddVideoSourceRequest request);
    DataResponse<Void> removeVideoSource(String slug, Long sourceId);
}