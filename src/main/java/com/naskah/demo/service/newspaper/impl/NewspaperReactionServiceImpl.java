package com.naskah.demo.service.newspaper.impl;

import com.naskah.demo.exception.custom.*;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.newspaper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.User;
import com.naskah.demo.model.entity.newspaper.*;
import com.naskah.demo.service.newspaper.NewspaperReactionService;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewspaperReactionServiceImpl implements NewspaperReactionService {

    private final NewspaperMapper newspaperMapper;
    private final ArticleRatingMapper articleRatingMapper;
    private final ArticleReviewMapper articleReviewMapper;
    private final ArticleReviewReplyMapper articleReviewReplyMapper;
    private final ArticleReviewFeedbackMapper articleReviewFeedbackMapper;
    private final SavedArticleMapper savedArticleMapper;
    private final ArticleShareMapper articleShareMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";

    // ============================================
    // ARTICLE RATING
    // ============================================

    @Override
    @Transactional
    public DataResponse<ArticleRatingResponse> addOrUpdateArticleRating(
            String categorySlug, LocalDate date, String articleSlug, RatingRequest request) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            ArticleRating existingRating = articleRatingMapper.findByUserAndArticle(
                    user.getId(), article.getId());

            ArticleRating savedRating;
            String message;
            int statusCode;

            if (existingRating != null) {
                existingRating.setRating(request.getRating());
                existingRating.setUpdatedAt(LocalDateTime.now());
                articleRatingMapper.update(existingRating);
                savedRating = existingRating;
                message = "Article rating updated successfully";
                statusCode = HttpStatus.OK.value();
            } else {
                ArticleRating rating = new ArticleRating();
                rating.setUserId(user.getId());
                rating.setArticleId(article.getId());
                rating.setRating(request.getRating());
                rating.setCreatedAt(LocalDateTime.now());
                rating.setUpdatedAt(LocalDateTime.now());
                articleRatingMapper.insert(rating);
                savedRating = rating;
                message = "Article rating added successfully";
                statusCode = HttpStatus.CREATED.value();
            }

            ArticleRatingResponse response = mapToArticleRatingResponse(savedRating, user);

            return new DataResponse<>(SUCCESS, message, statusCode, response);

        } catch (Exception e) {
            log.error("Error processing article rating for: {}", articleSlug, e);
            throw e;
        }
    }

    @Override
    public DataResponse<ArticleRatingStatsResponse> getArticleRatingStats(
            String categorySlug, LocalDate date, String articleSlug) {
        try {
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            ArticleRatingStatsResponse stats = articleRatingMapper.getArticleRatingStats(
                    article.getId());

            if (stats == null) {
                stats = createEmptyRatingStats(article.getId());
            }

            // Add user's rating if authenticated
            Long userId = getCurrentUserId();
            if (userId != null) {
                ArticleRating userRating = articleRatingMapper.findByUserAndArticle(userId, article.getId());
                stats.setMyRating(userRating != null ? userRating.getRating() : null);
            }

            return new DataResponse<>(SUCCESS, "Rating stats retrieved successfully", HttpStatus.OK.value(), stats);

        } catch (Exception e) {
            log.error("Error getting rating stats for: {}", articleSlug, e);
            throw e;
        }
    }

    @Override
    public DataResponse<ArticleRatingResponse> getMyArticleRating(
            String categorySlug, LocalDate date, String articleSlug) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            ArticleRating rating = articleRatingMapper.findByUserAndArticle(
                    user.getId(), article.getId());

            if (rating == null) {
                return new DataResponse<>(SUCCESS, "No rating found",
                        HttpStatus.OK.value(), null);
            }

            ArticleRatingResponse response = mapToArticleRatingResponse(rating, user);

            return new DataResponse<>(SUCCESS, "Rating retrieved successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error getting user rating for: {}", articleSlug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteArticleRating(
            String categorySlug, LocalDate date, String articleSlug) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            ArticleRating rating = articleRatingMapper.findByUserAndArticle(
                    user.getId(), article.getId());

            if (rating == null) {
                throw new DataNotFoundException();
            }

            articleRatingMapper.delete(rating.getId());

            return new DataResponse<>(SUCCESS, "Rating deleted successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting rating for: {}", articleSlug, e);
            throw e;
        }
    }

    // ============================================
    // ARTICLE REVIEWS
    // ============================================

    @Override
    public DatatableResponse<ArticleReviewResponse> getArticleReviews(
            String categorySlug, LocalDate date, String articleSlug,
            int page, int limit, String sortBy) {
        try {
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            int offset = (page - 1) * limit;
            List<ArticleReview> reviews = articleReviewMapper.findByArticleWithPagination(
                    article.getId(), offset, limit, sortBy);

            Long currentUserId = getCurrentUserId();
            List<ArticleReviewResponse> responses = reviews.stream()
                    .map(review -> mapToArticleReviewResponse(review, currentUserId))
                    .toList();

            int totalCount = articleReviewMapper.countByArticle(article.getId());

            PageDataResponse<ArticleReviewResponse> pageData =
                    new PageDataResponse<>(page, limit, totalCount, responses);

            return new DatatableResponse<>(SUCCESS, "Reviews retrieved successfully",
                    HttpStatus.OK.value(), pageData);

        } catch (Exception e) {
            log.error("Error getting reviews for: {}", articleSlug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<ArticleReviewResponse> createArticleReview(
            String categorySlug, LocalDate date, String articleSlug,
            ArticleReviewRequest request) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            ArticleReview existingReview = articleReviewMapper.findByUserAndArticle(
                    user.getId(), article.getId());

            if (existingReview != null) {
                throw new IllegalArgumentException(
                        "You already have a review for this article. Use update endpoint.");
            }

            ArticleReview review = new ArticleReview();
            review.setUserId(user.getId());
            review.setArticleId(article.getId());
            review.setTitle(request.getTitle());
            review.setContent(request.getContent());
            review.setHelpfulCount(0);
            review.setNotHelpfulCount(0);
            review.setReplyCount(0);
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());

            articleReviewMapper.insert(review);

            ArticleReviewResponse response = mapToArticleReviewResponse(review, user.getId());

            return new DataResponse<>(SUCCESS, "Review created successfully",
                    HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error creating review for: {}", articleSlug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<ArticleReviewResponse> updateMyArticleReview(
            String categorySlug, LocalDate date, String articleSlug,
            ArticleReviewRequest request) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            ArticleReview review = articleReviewMapper.findByUserAndArticle(
                    user.getId(), article.getId());

            if (review == null) {
                throw new DataNotFoundException();
            }

            review.setTitle(request.getTitle());
            review.setContent(request.getContent());
            review.setUpdatedAt(LocalDateTime.now());

            articleReviewMapper.update(review);

            ArticleReviewResponse response = mapToArticleReviewResponse(review, user.getId());

            return new DataResponse<>(SUCCESS, "Review updated successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating review for: {}", articleSlug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteMyArticleReview(
            String categorySlug, LocalDate date, String articleSlug) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            ArticleReview review = articleReviewMapper.findByUserAndArticle(
                    user.getId(), article.getId());

            if (review == null) {
                throw new DataNotFoundException();
            }

            articleReviewMapper.softDelete(review.getId());

            return new DataResponse<>(SUCCESS, "Review deleted successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting review for: {}", articleSlug, e);
            throw e;
        }
    }

    // ============================================
    // REVIEW REPLIES
    // ============================================

    @Override
    @Transactional
    public DataResponse<ArticleReviewReplyResponse> addReplyToReview(
            String categorySlug, LocalDate date, String articleSlug,
            Long reviewId, ReplyRequest request) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            ArticleReview review = articleReviewMapper.findById(reviewId);
            if (review == null || !review.getArticleId().equals(article.getId())) {
                throw new DataNotFoundException();
            }

            if (review.getUserId().equals(user.getId())) {
                throw new IllegalArgumentException("Cannot reply to your own review");
            }

            ArticleReviewReply reply = new ArticleReviewReply();
            reply.setUserId(user.getId());
            reply.setReviewId(reviewId);
            reply.setContent(request.getContent());
            reply.setCreatedAt(LocalDateTime.now());
            reply.setUpdatedAt(LocalDateTime.now());

            articleReviewReplyMapper.insert(reply);

            ArticleReviewReplyResponse response = mapToReviewReplyResponse(reply, user.getId());

            return new DataResponse<>(SUCCESS, "Reply added successfully",
                    HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error adding reply to review: {}", reviewId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<ArticleReviewReplyResponse> updateReply(
            Long replyId, ReplyRequest request) {
        try {
            User user = getAuthenticatedUser();

            ArticleReviewReply reply = articleReviewReplyMapper.findById(replyId);
            if (reply == null) {
                throw new DataNotFoundException();
            }

            if (!reply.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            reply.setContent(request.getContent());
            reply.setUpdatedAt(LocalDateTime.now());

            articleReviewReplyMapper.update(reply);

            ArticleReviewReplyResponse response = mapToReviewReplyResponse(reply, user.getId());

            return new DataResponse<>(SUCCESS, "Reply updated successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error updating reply: {}", replyId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteReply(Long replyId) {
        try {
            User user = getAuthenticatedUser();

            ArticleReviewReply reply = articleReviewReplyMapper.findById(replyId);
            if (reply == null) {
                throw new DataNotFoundException();
            }

            if (!reply.getUserId().equals(user.getId())) {
                throw new UnauthorizedException();
            }

            articleReviewReplyMapper.softDelete(replyId);

            return new DataResponse<>(SUCCESS, "Reply deleted successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting reply: {}", replyId, e);
            throw e;
        }
    }

    // ============================================
    // REVIEW FEEDBACK
    // ============================================

    @Override
    @Transactional
    public DataResponse<Void> addOrUpdateReviewFeedback(
            Long reviewId, FeedbackRequest request) {
        try {
            User user = getAuthenticatedUser();

            ArticleReview review = articleReviewMapper.findById(reviewId);
            if (review == null) {
                throw new DataNotFoundException();
            }

            if (review.getUserId().equals(user.getId())) {
                throw new IllegalArgumentException("Cannot give feedback to your own review");
            }

            ArticleReviewFeedback existing = articleReviewFeedbackMapper.findByUserAndReview(
                    user.getId(), reviewId);

            if (existing != null) {
                existing.setIsHelpful(request.getIsHelpful());
                existing.setUpdatedAt(LocalDateTime.now());
                articleReviewFeedbackMapper.update(existing);
            } else {
                ArticleReviewFeedback feedback = new ArticleReviewFeedback();
                feedback.setUserId(user.getId());
                feedback.setReviewId(reviewId);
                feedback.setIsHelpful(request.getIsHelpful());
                feedback.setCreatedAt(LocalDateTime.now());
                feedback.setUpdatedAt(LocalDateTime.now());
                articleReviewFeedbackMapper.insert(feedback);
            }

            return new DataResponse<>(SUCCESS, "Feedback saved successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error processing feedback for review: {}", reviewId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteReviewFeedback(Long reviewId) {
        try {
            User user = getAuthenticatedUser();

            ArticleReviewFeedback feedback = articleReviewFeedbackMapper.findByUserAndReview(
                    user.getId(), reviewId);

            if (feedback == null) {
                throw new DataNotFoundException();
            }

            articleReviewFeedbackMapper.delete(feedback.getId());

            return new DataResponse<>(SUCCESS, "Feedback deleted successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error deleting feedback for review: {}", reviewId, e);
            throw e;
        }
    }

    // ============================================
    // SAVE ARTICLE
    // ============================================

    @Override
    @Transactional
    public DataResponse<SavedArticleResponse> saveArticle(
            String categorySlug, LocalDate date, String articleSlug,
            SaveArticleRequest request) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            SavedArticle existing = savedArticleMapper.findByUserAndArticle(
                    user.getId(), article.getId());

            if (existing != null) {
                throw new IllegalArgumentException("Article already saved");
            }

            SavedArticle saved = new SavedArticle();
            saved.setUserId(user.getId());
            saved.setArticleId(article.getId());
            saved.setCollectionName(request.getCollectionName() != null ?
                    request.getCollectionName() : "default");
            saved.setNotes(request.getNotes());
            saved.setCreatedAt(LocalDateTime.now());

            savedArticleMapper.insert(saved);

            // Increment save count
            newspaperMapper.incrementSaveCount(article.getId());

            SavedArticleResponse response = mapToSavedArticleResponse(saved);

            return new DataResponse<>(SUCCESS, "Article saved successfully",
                    HttpStatus.CREATED.value(), response);

        } catch (Exception e) {
            log.error("Error saving article: {}", articleSlug, e);
            throw e;
        }
    }

    @Override
    public DataResponse<SavedArticleResponse> checkArticleSaved(
            String categorySlug, LocalDate date, String articleSlug) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            SavedArticle saved = savedArticleMapper.findByUserAndArticle(
                    user.getId(), article.getId());

            if (saved == null) {
                return new DataResponse<>(SUCCESS, "Article not saved",
                        HttpStatus.OK.value(), null);
            }

            SavedArticleResponse response = mapToSavedArticleResponse(saved);

            return new DataResponse<>(SUCCESS, "Saved article retrieved successfully",
                    HttpStatus.OK.value(), response);

        } catch (Exception e) {
            log.error("Error checking saved article: {}", articleSlug, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public DataResponse<Void> unsaveArticle(
            String categorySlug, LocalDate date, String articleSlug) {
        try {
            User user = getAuthenticatedUser();
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);

            SavedArticle saved = savedArticleMapper.findByUserAndArticle(
                    user.getId(), article.getId());

            if (saved == null) {
                throw new DataNotFoundException();
            }

            savedArticleMapper.delete(saved.getId());

            // Decrement save count
            newspaperMapper.decrementSaveCount(article.getId());

            return new DataResponse<>(SUCCESS, "Article unsaved successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error unsaving article: {}", articleSlug, e);
            throw e;
        }
    }

    // ============================================
    // SHARE TRACKING
    // ============================================

    @Override
    @Transactional
    public DataResponse<Void> trackArticleShare(
            String categorySlug, LocalDate date, String articleSlug,
            ShareArticleRequest request) {
        try {
            NewspaperArticle article = getArticleOrThrow(categorySlug, date, articleSlug);
            Long userId = getCurrentUserId();

            ArticleShare share = new ArticleShare();
            share.setArticleId(article.getId());
            share.setUserId(userId);
            share.setPlatform(request.getPlatform());
            share.setCreatedAt(LocalDateTime.now());

            articleShareMapper.insert(share);

            // Increment share count
            newspaperMapper.incrementShareCount(article.getId());

            return new DataResponse<>(SUCCESS, "Share tracked successfully",
                    HttpStatus.OK.value(), null);

        } catch (Exception e) {
            log.error("Error tracking share for: {}", articleSlug, e);
            throw e;
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private User getAuthenticatedUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.isEmpty()) {
            throw new UnauthorizedException();
        }

        User user = userMapper.findUserByUsername(username);
        if (user == null) {
            throw new UnauthorizedException();
        }

        return user;
    }

    private Long getCurrentUserId() {
        try {
            return getAuthenticatedUser().getId();
        } catch (UnauthorizedException e) {
            return null;
        }
    }

    private NewspaperArticle getArticleOrThrow(
            String categorySlug, LocalDate date, String articleSlug) {
        NewspaperArticle article = newspaperMapper.findArticleByCategoryDateAndSlug(
                categorySlug, date, articleSlug);

        if (article == null) {
            throw new DataNotFoundException();
        }

        return article;
    }

    private ArticleRatingStatsResponse createEmptyRatingStats(Long articleId) {
        ArticleRatingStatsResponse stats = new ArticleRatingStatsResponse();
        stats.setArticleId(articleId);
        stats.setAverageRating(BigDecimal.ZERO);
        stats.setTotalRatings(0);
        stats.setFiveStarCount(0);
        stats.setFourStarCount(0);
        stats.setThreeStarCount(0);
        stats.setTwoStarCount(0);
        stats.setOneStarCount(0);
        return stats;
    }

    private ArticleRatingResponse mapToArticleRatingResponse(ArticleRating rating, User user) {
        ArticleRatingResponse response = new ArticleRatingResponse();
        response.setId(rating.getId());
        response.setUserId(rating.getUserId());
        response.setUserName(user.getUsername());
        response.setUserPhotoUrl(user.getProfilePictureUrl());
        response.setArticleId(rating.getArticleId());
        response.setRating(rating.getRating());
        response.setCreatedAt(rating.getCreatedAt());
        response.setUpdatedAt(rating.getUpdatedAt());
        return response;
    }

    private ArticleReviewResponse mapToArticleReviewResponse(ArticleReview review, Long currentUserId) {
        User reviewUser = userMapper.findUserById(review.getUserId());

        ArticleReviewResponse response = new ArticleReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setUserName(reviewUser != null ? reviewUser.getUsername() : "Unknown");
        response.setUserPhotoUrl(reviewUser != null ? reviewUser.getProfilePictureUrl() : null);
        response.setArticleId(review.getArticleId());
        response.setTitle(review.getTitle());
        response.setContent(review.getContent());
        response.setHelpfulCount(review.getHelpfulCount());
        response.setNotHelpfulCount(review.getNotHelpfulCount());
        response.setReplyCount(review.getReplyCount());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        response.setIsOwner(currentUserId != null &&
                currentUserId.equals(review.getUserId()));

        if (currentUserId != null) {
            ArticleReviewFeedback feedback = articleReviewFeedbackMapper.findByUserAndReview(
                    currentUserId, review.getId());
            response.setCurrentUserFeedback(feedback != null ? feedback.getIsHelpful() : null);
        }

        List<ArticleReviewReply> replies = articleReviewReplyMapper.findByReviewId(
                review.getId());
        List<ArticleReviewReplyResponse> replyResponses = replies.stream()
                .map(reply -> mapToReviewReplyResponse(reply, currentUserId))
                .toList();
        response.setReplies(replyResponses);

        return response;
    }

    private ArticleReviewReplyResponse mapToReviewReplyResponse(
            ArticleReviewReply reply, Long currentUserId) {
        User replyUser = userMapper.findUserById(reply.getUserId());

        ArticleReviewReplyResponse response = new ArticleReviewReplyResponse();
        response.setId(reply.getId());
        response.setUserId(reply.getUserId());
        response.setUserName(replyUser != null ? replyUser.getUsername() : "Unknown");
        response.setUserPhotoUrl(replyUser != null ? replyUser.getProfilePictureUrl() : null);
        response.setReviewId(reply.getReviewId());
        response.setParentReplyId(reply.getParentReplyId());
        response.setContent(reply.getContent());
        response.setCreatedAt(reply.getCreatedAt());
        response.setUpdatedAt(reply.getUpdatedAt());
        response.setIsOwner(currentUserId != null &&
                currentUserId.equals(reply.getUserId()));

        return response;
    }

    private SavedArticleResponse mapToSavedArticleResponse(SavedArticle saved) {
        SavedArticleResponse response = new SavedArticleResponse();
        response.setId(saved.getId());
        response.setArticleId(saved.getArticleId());
        response.setCollectionName(saved.getCollectionName());
        response.setNotes(saved.getNotes());
        response.setSavedAt(saved.getCreatedAt());

        // Optionally load article data
        // response.setArticle(...);

        return response;
    }
}