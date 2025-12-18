package com.naskah.demo.service.book.impl;

import com.naskah.demo.exception.custom.*;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.book.BookReactionService;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookReactionServiceImpl implements BookReactionService {

    private final BookRatingMapper bookRatingMapper;
    private final BookReviewMapper bookReviewMapper;
    private final BookReviewReplyMapper bookReviewReplyMapper;
    private final BookReviewFeedbackMapper bookReviewFeedbackMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";

    // ============================================
    // BOOK RATING OPERATIONS
    // ============================================

    @Override
    @Transactional
    public DataResponse<BookRatingResponse> addOrUpdateBookRating(String slug, RatingRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            BookRating existingRating = bookRatingMapper.findByUserAndBook(user.getId(), book.getId());

            BookRating savedRating;
            String message;
            int statusCode;

            if (existingRating != null) {
                existingRating.setRating(request.getRating());
                existingRating.setUpdatedAt(LocalDateTime.now());
                bookRatingMapper.update(existingRating);
                savedRating = existingRating;
                message = "Book rating updated successfully";
                statusCode = HttpStatus.OK.value();
            } else {
                BookRating rating = new BookRating();
                rating.setUserId(user.getId());
                rating.setBookId(book.getId());
                rating.setRating(request.getRating());
                rating.setCreatedAt(LocalDateTime.now());
                rating.setUpdatedAt(LocalDateTime.now());
                bookRatingMapper.insert(rating);
                savedRating = rating;
                message = "Book rating added successfully";
                statusCode = HttpStatus.CREATED.value();
            }

            BookRatingResponse response = mapToBookRatingResponse(savedRating, user);
            return new DataResponse<>(SUCCESS, message, statusCode, response);

        } catch (Exception e) {
            log.error("Error processing book rating for: {}", slug, e);
            throw e;
        }
    }

    @Override
    public DataResponse<BookRatingStatsResponse> getBookRatingStats(String slug) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            BookRatingStatsResponse stats = bookRatingMapper.getBookRatingStats(book.getId());

            if (stats == null) {
                stats = new BookRatingStatsResponse();
                stats.setBookId(book.getId());
                stats.setAverageRating(0.0);
                stats.setTotalRatings(0L);
                stats.setRating50Count(0);
                stats.setRating45Count(0);
                stats.setRating40Count(0);
                stats.setRating35Count(0);
                stats.setRating30Count(0);
                stats.setRating25Count(0);
                stats.setRating20Count(0);
                stats.setRating15Count(0);
                stats.setRating10Count(0);
                stats.setRating05Count(0);
            }

            return new DataResponse<>(SUCCESS, "Book rating stats retrieved successfully",
                    HttpStatus.OK.value(), stats);

        } catch (Exception e) {
            log.error("Error getting book rating stats for: {}", slug, e);
            throw e;
        }
    }

    @Override
    public DataResponse<BookRatingResponse> getMyBookRating(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            BookRating rating = bookRatingMapper.findByUserAndBook(user.getId(), book.getId());

            if (rating == null) {
                return new DataResponse<>(SUCCESS, "No rating found", HttpStatus.OK.value(), null);
            }

            BookRatingResponse response = mapToBookRatingResponse(rating, user);
            return new DataResponse<>(SUCCESS, "Book rating retrieved successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting user book rating for: {}", slug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteBookRating(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            BookRating rating = bookRatingMapper.findByUserAndBook(user.getId(), book.getId());
            if (rating == null) {
                throw new DataNotFoundException();
            }

            bookRatingMapper.delete(rating.getId());
            return new DataResponse<>(SUCCESS, "Book rating deleted successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting book rating for: {}", slug, e);
            throw e;
        }
    }

    // ============================================
    // BOOK REVIEW OPERATIONS
    // ============================================

    @Override
    public DatatableResponse<BookReviewResponse> getBookReviews(String slug, int page, int limit, String sortBy) {
        try {
            Book book = bookMapper.findBookBySlug(slug);
            if (book == null) {
                throw new DataNotFoundException();
            }

            Long currentUserId = getCurrentUserId();

            int offset = (page - 1) * limit;
            List<BookReview> reviews = bookReviewMapper.findByBookWithPagination(
                    book.getId(), offset, limit, sortBy);

            List<BookReviewResponse> responses = reviews.stream()
                    .map(review -> mapToBookReviewResponse(review, currentUserId))
                    .collect(Collectors.toList());

            int totalReviews = bookReviewMapper.countByBook(book.getId());
            int totalPages = (int) Math.ceil((double) totalReviews / limit);

            PageDataResponse<BookReviewResponse> pageData = new PageDataResponse<>(
                    page, limit, totalReviews, responses);

            return new DatatableResponse<>(SUCCESS, "Book reviews retrieved successfully",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting book reviews for: {}", slug, e);
            throw e;
        }
    }

    @Override
    public DataResponse<BookReviewResponse> getMyBookReview(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            BookReview review = bookReviewMapper.findByUserAndBook(user.getId(), book.getId());

            if (review == null) {
                return new DataResponse<>(SUCCESS, "No review found", HttpStatus.OK.value(), null);
            }

            BookReviewResponse response = mapToBookReviewResponse(review, user.getId());
            return new DataResponse<>(SUCCESS, "Book review retrieved successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting user book review for: {}", slug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<BookReviewResponse> createBookReview(String slug, BookReviewRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            BookReview existingReview = bookReviewMapper.findByUserAndBook(user.getId(), book.getId());
            if (existingReview != null) {
                throw new IllegalArgumentException(
                        "You already have a review for this book. Use update endpoint to modify it.");
            }

            BookReview review = new BookReview();
            review.setUserId(user.getId());
            review.setBookId(book.getId());
            review.setTitle(request.getTitle());
            review.setContent(request.getContent());
            review.setHelpfulCount(0);
            review.setNotHelpfulCount(0);
            review.setReplyCount(0);
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());

            bookReviewMapper.insert(review);

            BookReviewResponse response = mapToBookReviewResponse(review, user.getId());
            return new DataResponse<>(SUCCESS, "Book review created successfully",
                    HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error creating book review for: {}", slug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<BookReviewResponse> updateBookReview(String slug, BookReviewRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            BookReview review = bookReviewMapper.findByUserAndBook(user.getId(), book.getId());
            if (review == null) {
                throw new DataNotFoundException();
            }

            review.setTitle(request.getTitle());
            review.setContent(request.getContent());
            review.setUpdatedAt(LocalDateTime.now());
            bookReviewMapper.update(review);

            BookReviewResponse response = mapToBookReviewResponse(review, user.getId());
            return new DataResponse<>(SUCCESS, "Book review updated successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating book review for: {}", slug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteBookReview(String slug) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            BookReview review = bookReviewMapper.findByUserAndBook(user.getId(), book.getId());
            if (review == null) {
                throw new DataNotFoundException();
            }

            bookReviewMapper.softDelete(review.getId());

            return new DataResponse<>(SUCCESS, "Book review deleted successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting book review for: {}", slug, e);
            throw e;
        }
    }

    // ============================================
    // BOOK REVIEW REPLY OPERATIONS
    // ============================================

    @Override
    @Transactional
    public DataResponse<BookReviewReplyResponse> addReplyToBookReview(
            String slug, Long reviewId, ReplyRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            BookReview review = bookReviewMapper.findById(reviewId);
            if (review == null) {
                throw new DataNotFoundException();
            }

            if (review.getUserId().equals(user.getId())) {
                throw new IllegalArgumentException("Cannot reply to your own review");
            }

            if (!review.getBookId().equals(book.getId())) {
                throw new IllegalArgumentException("Review does not belong to this book");
            }

            BookReviewReply reply = new BookReviewReply();
            reply.setUserId(user.getId());
            reply.setReviewId(reviewId);
            reply.setParentReplyId(null);
            reply.setContent(request.getContent());
            reply.setCreatedAt(LocalDateTime.now());
            reply.setUpdatedAt(LocalDateTime.now());

            bookReviewReplyMapper.insert(reply);

            BookReviewReplyResponse response = mapToBookReviewReplyResponse(reply, user.getId());
            return new DataResponse<>(SUCCESS, "Reply added successfully",
                    HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding reply to book review: {}", reviewId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<BookReviewReplyResponse> updateBookReviewReply(
            String slug, Long replyId, ReplyRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);

            if (user == null) {
                throw new DataNotFoundException();
            }

            BookReviewReply reply = bookReviewReplyMapper.findById(replyId);
            if (reply == null) {
                throw new DataNotFoundException();
            }

            if (!reply.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            reply.setContent(request.getContent());
            reply.setUpdatedAt(LocalDateTime.now());
            bookReviewReplyMapper.update(reply);

            BookReviewReplyResponse response = mapToBookReviewReplyResponse(reply, user.getId());
            return new DataResponse<>(SUCCESS, "Reply updated successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating book review reply: {}", replyId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteBookReviewReply(String slug, Long replyId) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);

            if (user == null) {
                throw new DataNotFoundException();
            }

            BookReviewReply reply = bookReviewReplyMapper.findById(replyId);
            if (reply == null) {
                throw new DataNotFoundException();
            }

            if (!reply.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            bookReviewReplyMapper.softDelete(replyId);

            return new DataResponse<>(SUCCESS, "Reply deleted successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting book review reply: {}", replyId, e);
            throw e;
        }
    }

    // ============================================
    // BOOK REVIEW FEEDBACK OPERATIONS
    // ============================================

    @Override
    @Transactional
    public DataResponse<Void> addOrUpdateBookReviewFeedback(
            String slug, Long reviewId, FeedbackRequest request) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);
            Book book = bookMapper.findBookBySlug(slug);

            if (user == null || book == null) {
                throw new DataNotFoundException();
            }

            BookReview review = bookReviewMapper.findById(reviewId);
            if (review == null) {
                throw new DataNotFoundException();
            }

            if (review.getUserId().equals(user.getId())) {
                throw new IllegalArgumentException("Cannot give feedback to your own review");
            }

            BookReviewFeedback existingFeedback = bookReviewFeedbackMapper.findByUserAndReview(
                    user.getId(), reviewId);

            String message;

            if (existingFeedback != null) {
                existingFeedback.setIsHelpful(request.getIsHelpful());
                existingFeedback.setUpdatedAt(LocalDateTime.now());
                bookReviewFeedbackMapper.update(existingFeedback);
                message = "Feedback updated successfully";
            } else {
                BookReviewFeedback feedback = new BookReviewFeedback();
                feedback.setUserId(user.getId());
                feedback.setReviewId(reviewId);
                feedback.setIsHelpful(request.getIsHelpful());
                feedback.setCreatedAt(LocalDateTime.now());
                feedback.setUpdatedAt(LocalDateTime.now());
                bookReviewFeedbackMapper.insert(feedback);
                message = "Feedback added successfully";
            }

            return new DataResponse<>(SUCCESS, message, HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error processing book review feedback: {}", reviewId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteBookReviewFeedback(String slug, Long reviewId) {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isEmpty()) {
                throw new UnauthorizedException();
            }

            User user = userMapper.findUserByUsername(username);

            if (user == null) {
                throw new DataNotFoundException();
            }

            BookReviewFeedback feedback = bookReviewFeedbackMapper.findByUserAndReview(
                    user.getId(), reviewId);
            if (feedback == null) {
                throw new DataNotFoundException();
            }

            bookReviewFeedbackMapper.delete(feedback.getId());

            return new DataResponse<>(SUCCESS, "Feedback deleted successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting book review feedback: {}", reviewId, e);
            throw e;
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private Long getCurrentUserId() {
        try {
            String username = headerHolder.getUsername();
            if (username != null) {
                User user = userMapper.findUserByUsername(username);
                if (user != null) {
                    return user.getId();
                }
            }
        } catch (Exception e) {
            // Guest user
        }
        return null;
    }

    private BookRatingResponse mapToBookRatingResponse(BookRating rating, User user) {
        BookRatingResponse response = new BookRatingResponse();
        response.setId(rating.getId());
        response.setUserId(rating.getUserId());
        response.setUserName(user.getUsername());
        response.setUserPhotoUrl(user.getProfilePictureUrl());
        response.setBookId(rating.getBookId());
        response.setRating(rating.getRating());
        response.setCreatedAt(rating.getCreatedAt());
        response.setUpdatedAt(rating.getUpdatedAt());
        return response;
    }

    private BookReviewResponse mapToBookReviewResponse(BookReview review, Long currentUserId) {
        User reviewUser = userMapper.findUserById(review.getUserId());

        BookReviewResponse response = new BookReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setUserName(reviewUser != null ? reviewUser.getUsername() : "Unknown");
        response.setUserPhotoUrl(reviewUser != null ? reviewUser.getProfilePictureUrl() : null);
        response.setBookId(review.getBookId());
        response.setTitle(review.getTitle());
        response.setContent(review.getContent());
        response.setHelpfulCount(review.getHelpfulCount());
        response.setNotHelpfulCount(review.getNotHelpfulCount());
        response.setReplyCount(review.getReplyCount());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        response.setIsOwner(currentUserId != null && currentUserId.equals(review.getUserId()));

        if (currentUserId != null) {
            BookReviewFeedback userFeedback = bookReviewFeedbackMapper.findByUserAndReview(
                    currentUserId, review.getId());
            response.setCurrentUserFeedback(
                    userFeedback != null ? userFeedback.getIsHelpful() : null);
        } else {
            response.setCurrentUserFeedback(null);
        }

        List<BookReviewReply> replies = bookReviewReplyMapper.findByReviewId(review.getId());
        List<BookReviewReplyResponse> replyResponses = replies.stream()
                .map(reply -> mapToBookReviewReplyResponse(reply, currentUserId))
                .collect(Collectors.toList());
        response.setReplies(replyResponses);

        return response;
    }

    private BookReviewReplyResponse mapToBookReviewReplyResponse(
            BookReviewReply reply, Long currentUserId) {
        User replyUser = userMapper.findUserById(reply.getUserId());

        BookReviewReplyResponse response = new BookReviewReplyResponse();
        response.setId(reply.getId());
        response.setUserId(reply.getUserId());
        response.setUserName(replyUser != null ? replyUser.getUsername() : "Unknown");
        response.setUserPhotoUrl(replyUser != null ? replyUser.getProfilePictureUrl() : null);
        response.setReviewId(reply.getReviewId());
        response.setParentReplyId(reply.getParentReplyId());
        response.setContent(reply.getContent());
        response.setCreatedAt(reply.getCreatedAt());
        response.setUpdatedAt(reply.getUpdatedAt());
        response.setIsOwner(currentUserId != null && currentUserId.equals(reply.getUserId()));

        return response;
    }
}