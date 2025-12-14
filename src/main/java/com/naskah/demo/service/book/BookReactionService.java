package com.naskah.demo.service.book;

import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;

/**
 * Service untuk handle BOOK-LEVEL reactions
 */
public interface BookReactionService {

    // Book Rating operations
    DataResponse<BookRatingResponse> addOrUpdateBookRating(String slug, RatingRequest request);
    DataResponse<BookRatingStatsResponse> getBookRatingStats(String slug);
    DataResponse<BookRatingResponse> getMyBookRating(String slug);
    DataResponse<Void> deleteBookRating(String slug);

    // Book Review operations
    DatatableResponse<BookReviewResponse> getBookReviews(String slug, int page, int limit, String sortBy);
    DataResponse<BookReviewResponse> getMyBookReview(String slug);
    DataResponse<BookReviewResponse> createBookReview(String slug, BookReviewRequest request);
    DataResponse<BookReviewResponse> updateBookReview(String slug, BookReviewRequest request);
    DataResponse<Void> deleteBookReview(String slug);

    // Book Review Reply operations
    DataResponse<BookReviewReplyResponse> addReplyToBookReview(String slug, Long reviewId, ReplyRequest request);
    DataResponse<BookReviewReplyResponse> updateBookReviewReply(String slug, Long replyId, ReplyRequest request);
    DataResponse<Void> deleteBookReviewReply(String slug, Long replyId);

    // Book Review Feedback operations
    DataResponse<Void> addOrUpdateBookReviewFeedback(String slug, Long reviewId, FeedbackRequest request);
    DataResponse<Void> deleteBookReviewFeedback(String slug, Long reviewId);
}