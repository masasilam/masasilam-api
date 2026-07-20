package com.masasilam.app.service.zine;

import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;

public interface ZineReactionService {
    DataResponse<ZineRatingResponse> addOrUpdateZineRating(String slug, RatingRequest request);
    DataResponse<ZineRatingStatsResponse> getZineRatingStats(String slug);
    DataResponse<ZineRatingResponse> getMyZineRating(String slug);
    DataResponse<Void> deleteZineRating(String slug);
    DatatableResponse<ZineReviewResponse> getZineReviews(String slug, int page, int limit, String sortBy);
    DataResponse<ZineReviewResponse> getMyZineReview(String slug);
    DataResponse<ZineReviewResponse> createZineReview(String slug, BookReviewRequest request);
    DataResponse<ZineReviewResponse> updateZineReview(String slug, BookReviewRequest request);
    DataResponse<Void> deleteZineReview(String slug);
    DataResponse<ZineReviewReplyResponse> addReplyToZineReview(String slug, Long reviewId, ReplyRequest request);
    DataResponse<ZineReviewReplyResponse> updateZineReviewReply(String slug, Long replyId, ReplyRequest request);
    DataResponse<Void> deleteZineReviewReply(String slug, Long replyId);
    DataResponse<Void> addOrUpdateZineReviewFeedback(String slug, Long reviewId, FeedbackRequest request);
    DataResponse<Void> deleteZineReviewFeedback(String slug, Long reviewId);
}