package com.naskah.demo.service.zine.impl;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.exception.custom.UnauthorizedException;
import com.naskah.demo.mapper.*;
import com.naskah.demo.model.dto.request.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.*;
import com.naskah.demo.service.zine.ZineReactionService;
import com.naskah.demo.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZineReactionServiceImpl implements ZineReactionService {

    private final ZineRatingMapper zineRatingMapper;
    private final ZineReviewMapper zineReviewMapper;
    private final ZineReviewReplyMapper zineReviewReplyMapper;
    private final ZineReviewFeedbackMapper zineReviewFeedbackMapper;
    private final ZineMapper zineMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";

    // ============================================
    // RATING
    // ============================================

    @Override
    @Transactional
    public DataResponse<ZineRatingResponse> addOrUpdateZineRating(String slug, RatingRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        Zine zine = requireZine(slug);

        ZineRating existingRating = zineRatingMapper.findByUserAndZine(user.getId(), zine.getId());
        ZineRating savedRating;
        String message;
        int statusCode;

        if (existingRating != null) {
            existingRating.setRating(request.getRating());
            existingRating.setUpdatedAt(LocalDateTime.now());
            zineRatingMapper.update(existingRating);
            savedRating = existingRating;
            message = "Rating zine diperbarui";
            statusCode = HttpStatus.OK.value();
        } else {
            ZineRating rating = new ZineRating();
            rating.setUserId(user.getId());
            rating.setZineId(zine.getId());
            rating.setRating(request.getRating());
            rating.setCreatedAt(LocalDateTime.now());
            rating.setUpdatedAt(LocalDateTime.now());
            zineRatingMapper.insert(rating);
            savedRating = rating;
            message = "Rating zine ditambahkan";
            statusCode = HttpStatus.CREATED.value();
        }

        return new DataResponse<>(SUCCESS, message, statusCode, mapToRatingResponse(savedRating, user));
    }

    @Override
    public DataResponse<ZineRatingStatsResponse> getZineRatingStats(String slug) {
        Zine zine = requireZine(slug);
        ZineRatingStatsResponse stats = zineRatingMapper.getZineRatingStats(zine.getId());

        if (stats == null) {
            stats = new ZineRatingStatsResponse();
            stats.setZineId(zine.getId());
            stats.setAverageRating(0.0);
            stats.setTotalRatings(0L);
            stats.setRating50Count(0); stats.setRating45Count(0); stats.setRating40Count(0);
            stats.setRating35Count(0); stats.setRating30Count(0); stats.setRating25Count(0);
            stats.setRating20Count(0); stats.setRating15Count(0); stats.setRating10Count(0);
            stats.setRating05Count(0);
        }

        return new DataResponse<>(SUCCESS, "Rating stats zine berhasil diambil", HttpStatus.OK.value(), stats);
    }

    @Override
    public DataResponse<ZineRatingResponse> getMyZineRating(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Zine zine = requireZine(slug);

        ZineRating rating = zineRatingMapper.findByUserAndZine(user.getId(), zine.getId());
        if (rating == null) return new DataResponse<>(SUCCESS, "Belum ada rating", HttpStatus.OK.value(), null);

        return new DataResponse<>(SUCCESS, "Rating zine berhasil diambil", HttpStatus.OK.value(),
                mapToRatingResponse(rating, user));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteZineRating(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Zine zine = requireZine(slug);

        ZineRating rating = zineRatingMapper.findByUserAndZine(user.getId(), zine.getId());
        if (rating == null) throw new DataNotFoundException();

        zineRatingMapper.delete(rating.getId());
        return new DataResponse<>(SUCCESS, "Rating zine dihapus", HttpStatus.OK.value(), null);
    }

    // ============================================
    // REVIEW
    // ============================================

    @Override
    public DatatableResponse<ZineReviewResponse> getZineReviews(String slug, int page, int limit, String sortBy) {
        Zine zine = requireZine(slug);
        Long currentUserId = getCurrentUserId();
        int offset = (page - 1) * limit;

        List<ZineReview> reviews = zineReviewMapper.findByZineWithPagination(zine.getId(), offset, limit, sortBy);
        List<ZineReviewResponse> responses = reviews.stream()
                .map(r -> mapToReviewResponse(r, currentUserId)).toList();
        int total = zineReviewMapper.countByZine(zine.getId());

        return new DatatableResponse<>(SUCCESS, "Review zine berhasil diambil",
                HttpStatus.OK.value(), new PageDataResponse<>(page, limit, total, responses));
    }

    @Override
    public DataResponse<ZineReviewResponse> getMyZineReview(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Zine zine = requireZine(slug);

        ZineReview review = zineReviewMapper.findByUserAndZine(user.getId(), zine.getId());
        if (review == null) return new DataResponse<>(SUCCESS, "Belum ada review", HttpStatus.OK.value(), null);

        return new DataResponse<>(SUCCESS, "Review zine berhasil diambil", HttpStatus.OK.value(),
                mapToReviewResponse(review, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<ZineReviewResponse> createZineReview(String slug, BookReviewRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        Zine zine = requireZine(slug);

        if (zineReviewMapper.findByUserAndZine(user.getId(), zine.getId()) != null) {
            throw new IllegalArgumentException("Kamu sudah punya review untuk zine ini. Gunakan endpoint update.");
        }

        ZineReview review = new ZineReview();
        review.setUserId(user.getId());
        review.setZineId(zine.getId());
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());
        review.setHelpfulCount(0);
        review.setNotHelpfulCount(0);
        review.setReplyCount(0);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        zineReviewMapper.insert(review);

        return new DataResponse<>(SUCCESS, "Review zine dibuat", HttpStatus.CREATED.value(),
                mapToReviewResponse(review, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<ZineReviewResponse> updateZineReview(String slug, BookReviewRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        Zine zine = requireZine(slug);

        ZineReview review = zineReviewMapper.findByUserAndZine(user.getId(), zine.getId());
        if (review == null) throw new DataNotFoundException();

        review.setTitle(request.getTitle());
        review.setContent(request.getContent());
        review.setUpdatedAt(LocalDateTime.now());
        zineReviewMapper.update(review);

        return new DataResponse<>(SUCCESS, "Review zine diperbarui", HttpStatus.OK.value(),
                mapToReviewResponse(review, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteZineReview(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Zine zine = requireZine(slug);

        ZineReview review = zineReviewMapper.findByUserAndZine(user.getId(), zine.getId());
        if (review == null) throw new DataNotFoundException();

        zineReviewMapper.softDelete(review.getId());
        return new DataResponse<>(SUCCESS, "Review zine dihapus", HttpStatus.OK.value(), null);
    }

    // ============================================
    // REPLY
    // ============================================

    @Override
    @Transactional
    public DataResponse<ZineReviewReplyResponse> addReplyToZineReview(String slug, Long reviewId,
                                                                      ReplyRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        Zine zine = requireZine(slug);

        ZineReview review = zineReviewMapper.findById(reviewId);
        if (review == null) throw new DataNotFoundException();
        if (review.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Tidak bisa reply ke review sendiri");
        }
        if (!review.getZineId().equals(zine.getId())) {
            throw new IllegalArgumentException("Review tidak milik zine ini");
        }

        ZineReviewReply reply = new ZineReviewReply();
        reply.setUserId(user.getId());
        reply.setReviewId(reviewId);
        reply.setParentReplyId(null);
        reply.setContent(request.getContent());
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());
        zineReviewReplyMapper.insert(reply);

        return new DataResponse<>(SUCCESS, "Reply ditambahkan", HttpStatus.CREATED.value(),
                mapToReplyResponse(reply, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<ZineReviewReplyResponse> updateZineReviewReply(String slug, Long replyId,
                                                                       ReplyRequest request) {
        String username = requireUsername();
        User user = requireUser(username);

        ZineReviewReply reply = zineReviewReplyMapper.findById(replyId);
        if (reply == null) throw new DataNotFoundException();
        if (!reply.getUserId().equals(user.getId())) throw new UnauthorizedException();

        reply.setContent(request.getContent());
        reply.setUpdatedAt(LocalDateTime.now());
        zineReviewReplyMapper.update(reply);

        return new DataResponse<>(SUCCESS, "Reply diperbarui", HttpStatus.OK.value(),
                mapToReplyResponse(reply, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteZineReviewReply(String slug, Long replyId) {
        String username = requireUsername();
        User user = requireUser(username);

        ZineReviewReply reply = zineReviewReplyMapper.findById(replyId);
        if (reply == null) throw new DataNotFoundException();
        if (!reply.getUserId().equals(user.getId())) throw new UnauthorizedException();

        zineReviewReplyMapper.softDelete(replyId);
        return new DataResponse<>(SUCCESS, "Reply dihapus", HttpStatus.OK.value(), null);
    }

    // ============================================
    // FEEDBACK
    // ============================================

    @Override
    @Transactional
    public DataResponse<Void> addOrUpdateZineReviewFeedback(String slug, Long reviewId,
                                                            FeedbackRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        requireZine(slug);

        ZineReview review = zineReviewMapper.findById(reviewId);
        if (review == null) throw new DataNotFoundException();
        if (review.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Tidak bisa memberi feedback ke review sendiri");
        }

        ZineReviewFeedback existing = zineReviewFeedbackMapper.findByUserAndReview(user.getId(), reviewId);
        String message;

        if (existing != null) {
            existing.setIsHelpful(request.getIsHelpful());
            existing.setUpdatedAt(LocalDateTime.now());
            zineReviewFeedbackMapper.update(existing);
            message = "Feedback diperbarui";
        } else {
            ZineReviewFeedback feedback = new ZineReviewFeedback();
            feedback.setUserId(user.getId());
            feedback.setReviewId(reviewId);
            feedback.setIsHelpful(request.getIsHelpful());
            feedback.setCreatedAt(LocalDateTime.now());
            feedback.setUpdatedAt(LocalDateTime.now());
            zineReviewFeedbackMapper.insert(feedback);
            message = "Feedback ditambahkan";
        }

        return new DataResponse<>(SUCCESS, message, HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteZineReviewFeedback(String slug, Long reviewId) {
        String username = requireUsername();
        User user = requireUser(username);

        ZineReviewFeedback feedback = zineReviewFeedbackMapper.findByUserAndReview(user.getId(), reviewId);
        if (feedback == null) throw new DataNotFoundException();

        zineReviewFeedbackMapper.delete(feedback.getId());
        return new DataResponse<>(SUCCESS, "Feedback dihapus", HttpStatus.OK.value(), null);
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private String requireUsername() {
        String username = headerHolder.getUsername();
        if (username == null || username.isEmpty()) throw new UnauthorizedException();
        return username;
    }

    private User requireUser(String username) {
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new DataNotFoundException();
        return user;
    }

    private Zine requireZine(String slug) {
        Zine zine = zineMapper.findZineBySlug(slug);
        if (zine == null) throw new DataNotFoundException();
        return zine;
    }

    private Long getCurrentUserId() {
        String username = headerHolder.getUsername();
        if (username != null) {
            User user = userMapper.findUserByUsername(username);
            if (user != null) return user.getId();
        }
        return null;
    }

    private ZineRatingResponse mapToRatingResponse(ZineRating rating, User user) {
        ZineRatingResponse r = new ZineRatingResponse();
        r.setId(rating.getId());
        r.setUserId(rating.getUserId());
        r.setUserName(user.getUsername());
        r.setUserPhotoUrl(user.getProfilePictureUrl());
        r.setZineId(rating.getZineId());
        r.setRating(rating.getRating());
        r.setCreatedAt(rating.getCreatedAt());
        r.setUpdatedAt(rating.getUpdatedAt());
        return r;
    }

    private ZineReviewResponse mapToReviewResponse(ZineReview review, Long currentUserId) {
        User reviewUser = userMapper.findUserById(review.getUserId());

        ZineReviewResponse r = new ZineReviewResponse();
        r.setId(review.getId());
        r.setUserId(review.getUserId());
        r.setUserName(reviewUser != null ? reviewUser.getUsername() : "Unknown");
        r.setUserPhotoUrl(reviewUser != null ? reviewUser.getProfilePictureUrl() : null);
        r.setZineId(review.getZineId());
        r.setTitle(review.getTitle());
        r.setContent(review.getContent());
        r.setHelpfulCount(review.getHelpfulCount());
        r.setNotHelpfulCount(review.getNotHelpfulCount());
        r.setReplyCount(review.getReplyCount());
        r.setCreatedAt(review.getCreatedAt());
        r.setUpdatedAt(review.getUpdatedAt());
        r.setIsOwner(currentUserId != null && currentUserId.equals(review.getUserId()));

        if (currentUserId != null) {
            ZineReviewFeedback feedback = zineReviewFeedbackMapper.findByUserAndReview(currentUserId, review.getId());
            r.setCurrentUserFeedback(feedback != null ? feedback.getIsHelpful() : null);
        }

        List<ZineReviewReply> replies = zineReviewReplyMapper.findByReviewId(review.getId());
        r.setReplies(replies.stream().map(rep -> mapToReplyResponse(rep, currentUserId)).toList());

        return r;
    }

    private ZineReviewReplyResponse mapToReplyResponse(ZineReviewReply reply, Long currentUserId) {
        User replyUser = userMapper.findUserById(reply.getUserId());

        ZineReviewReplyResponse r = new ZineReviewReplyResponse();
        r.setId(reply.getId());
        r.setUserId(reply.getUserId());
        r.setUserName(replyUser != null ? replyUser.getUsername() : "Unknown");
        r.setUserPhotoUrl(replyUser != null ? replyUser.getProfilePictureUrl() : null);
        r.setReviewId(reply.getReviewId());
        r.setParentReplyId(reply.getParentReplyId());
        r.setContent(reply.getContent());
        r.setCreatedAt(reply.getCreatedAt());
        r.setUpdatedAt(reply.getUpdatedAt());
        r.setIsOwner(currentUserId != null && currentUserId.equals(reply.getUserId()));
        return r;
    }
}