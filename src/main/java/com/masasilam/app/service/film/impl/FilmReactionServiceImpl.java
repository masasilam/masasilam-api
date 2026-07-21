package com.masasilam.app.service.film.impl;

import com.masasilam.app.exception.custom.DataNotFoundException;
import com.masasilam.app.exception.custom.ForbiddenException;
import com.masasilam.app.exception.custom.UnauthorizedException;
import com.masasilam.app.mapper.film.*;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.film.*;
import com.masasilam.app.service.film.FilmReactionService;
import com.masasilam.app.service.film.video.VideoProviderService;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmReactionServiceImpl implements FilmReactionService {
    private final FilmRatingMapper filmRatingMapper;
    private final FilmReviewMapper filmReviewMapper;
    private final FilmReviewReplyMapper filmReviewReplyMapper;
    private final FilmReviewFeedbackMapper filmReviewFeedbackMapper;
    private final FilmWatchlistMapper filmWatchlistMapper;
    private final WatchHistoryMapper watchHistoryMapper;
    private final FilmVideoSourceMapper filmVideoSourceMapper;
    private final FilmMapper filmMapper;
    private final UserMapper userMapper;
    private final VideoProviderService videoProviderService;
    private final HeaderHolder headerHolder;
    private static final String SUCCESS = "Success";

    private String requireUsername() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        return username;
    }

    private User requireUser(String username) {
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private Film requireFilm(String slug) {
        Film film = filmMapper.findBySlug(slug);
        if (film == null) throw new DataNotFoundException();
        return film;
    }

    private Long getCurrentUserId() {
        try {
            String username = headerHolder.getUsername();
            if (username != null && !username.isBlank()) {
                User user = userMapper.findUserByUsername(username);
                return user != null ? user.getId() : null;
            }
        } catch (Exception ignored) {
            log.warn("Failed to get current user ID");
        }
        return null;
    }

    private void requireAdmin() {
        String[] roles = headerHolder.getRoles();
        if (roles == null || Arrays.stream(roles).noneMatch("ADMIN"::equals)) {
            throw new ForbiddenException();
        }
    }

    @Override
    @Transactional
    public DataResponse<FilmRatingResponse> addOrUpdateRating(String slug, RatingRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        FilmRating existing = filmRatingMapper.findByUserAndFilm(user.getId(), film.getId());

        FilmRating saved;
        String message;
        int statusCode;

        if (existing != null) {
            existing.setRating(request.getRating());
            existing.setUpdatedAt(LocalDateTime.now());
            filmRatingMapper.update(existing);
            saved = existing;
            message = "Rating berhasil diperbarui";
            statusCode = HttpStatus.OK.value();
        } else {
            FilmRating newRating = FilmRating.builder()
                    .filmId(film.getId())
                    .userId(user.getId())
                    .rating(request.getRating())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            filmRatingMapper.insert(newRating);
            saved = newRating;
            message = "Rating berhasil ditambahkan";
            statusCode = HttpStatus.CREATED.value();
        }

        log.info("[Rating] user={} film={} rating={}", user.getId(), film.getId(), saved.getRating());
        return new DataResponse<>(SUCCESS, message, statusCode, mapToRatingResponse(saved, user));
    }

    @Override
    public DataResponse<FilmRatingStatsResponse> getRatingStats(String slug) {
        Film film = requireFilm(slug);
        FilmRatingStatsResponse stats = filmRatingMapper.getFilmRatingStats(film.getId());

        if (stats == null) {
            stats = new FilmRatingStatsResponse();
            stats.setFilmId(film.getId());
            stats.setAverageRating(0.0);
            stats.setTotalRatings(0L);
            stats.setCount50(0);
            stats.setCount45(0);
            stats.setCount40(0);
            stats.setCount35(0);
            stats.setCount30(0);
            stats.setCount25(0);
            stats.setCount20(0);
            stats.setCount15(0);
            stats.setCount10(0);
            stats.setCount05(0);
        }

        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("5.0", nullToZero(stats.getCount50()));
        dist.put("4.5", nullToZero(stats.getCount45()));
        dist.put("4.0", nullToZero(stats.getCount40()));
        dist.put("3.5", nullToZero(stats.getCount35()));
        dist.put("3.0", nullToZero(stats.getCount30()));
        dist.put("2.5", nullToZero(stats.getCount25()));
        dist.put("2.0", nullToZero(stats.getCount20()));
        dist.put("1.5", nullToZero(stats.getCount15()));
        dist.put("1.0", nullToZero(stats.getCount10()));
        dist.put("0.5", nullToZero(stats.getCount05()));
        stats.setDistribution(dist);

        return new DataResponse<>(SUCCESS, "Rating stats berhasil diambil", HttpStatus.OK.value(), stats);
    }

    @Override
    public DataResponse<FilmRatingResponse> getMyRating(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        FilmRating rating = filmRatingMapper.findByUserAndFilm(user.getId(), film.getId());
        if (rating == null) {
            return new DataResponse<>(SUCCESS, "Belum ada rating", HttpStatus.OK.value(), null);
        }
        return new DataResponse<>(SUCCESS, "Rating berhasil diambil",
                HttpStatus.OK.value(), mapToRatingResponse(rating, user));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteRating(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        FilmRating rating = filmRatingMapper.findByUserAndFilm(user.getId(), film.getId());
        if (rating == null) throw new DataNotFoundException();

        filmRatingMapper.delete(rating.getId());
        return new DataResponse<>(SUCCESS, "Rating dihapus", HttpStatus.OK.value(), null);
    }

    @Override
    public DatatableResponse<FilmReviewResponse> getReviews(String slug, int page, int limit, String sortBy) {
        Film film = requireFilm(slug);
        Long currentUserId = getCurrentUserId();
        int offset = (page - 1) * limit;

        List<FilmReview> reviews = filmReviewMapper.findByFilmWithPagination(film.getId(), offset, limit, sortBy);
        int total = filmReviewMapper.countByFilm(film.getId());

        List<FilmReviewResponse> responses = reviews.stream()
                .map(r -> mapToReviewResponse(r, currentUserId))
                .toList();

        PageDataResponse<FilmReviewResponse> pageData = new PageDataResponse<>(page, limit, total, responses);

        return new DatatableResponse<>(SUCCESS, "Ulasan berhasil diambil", HttpStatus.OK.value(), pageData);
    }

    @Override
    public DataResponse<FilmReviewResponse> getMyReview(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        FilmReview review = filmReviewMapper.findByUserAndFilm(user.getId(), film.getId());
        if (review == null) {
            return new DataResponse<>(SUCCESS, "Belum ada ulasan", HttpStatus.OK.value(), null);
        }
        return new DataResponse<>(SUCCESS, "Ulasan berhasil diambil", HttpStatus.OK.value(), mapToReviewResponse(review, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<FilmReviewResponse> createReview(String slug, FilmReviewRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        if (filmReviewMapper.findByUserAndFilm(user.getId(), film.getId()) != null) {
            throw new IllegalArgumentException("Kamu sudah menulis ulasan untuk film ini. Gunakan endpoint PUT untuk memperbarui.");
        }

        FilmReview review = FilmReview.builder()
                .filmId(film.getId())
                .userId(user.getId())
                .title(request.getTitle())
                .content(request.getContent())
                .helpfulCount(0)
                .notHelpfulCount(0)
                .replyCount(0)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        filmReviewMapper.insert(review);
        log.info("[Review] Created reviewId={} by user={} for film={}", review.getId(), user.getId(), film.getId());

        return new DataResponse<>(SUCCESS, "Ulasan berhasil dibuat", HttpStatus.CREATED.value(), mapToReviewResponse(review, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<FilmReviewResponse> updateReview(String slug, FilmReviewRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        FilmReview review = filmReviewMapper.findByUserAndFilm(user.getId(), film.getId());
        if (review == null) throw new DataNotFoundException();

        review.setTitle(request.getTitle());
        review.setContent(request.getContent());
        review.setUpdatedAt(LocalDateTime.now());
        filmReviewMapper.update(review);

        return new DataResponse<>(SUCCESS, "Ulasan berhasil diperbarui", HttpStatus.OK.value(), mapToReviewResponse(review, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteReview(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        FilmReview review = filmReviewMapper.findByUserAndFilm(user.getId(), film.getId());
        if (review == null) throw new DataNotFoundException();

        filmReviewMapper.softDelete(review.getId());
        return new DataResponse<>(SUCCESS, "Ulasan dihapus", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<FilmReviewReplyResponse> addReply(String slug, Long reviewId, ReplyRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        requireFilm(slug);

        FilmReview review = filmReviewMapper.findById(reviewId);
        if (review == null) throw new DataNotFoundException();

        if (review.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Kamu tidak bisa membalas ulasan sendiri");
        }

        FilmReviewReply reply = FilmReviewReply.builder()
                .reviewId(reviewId)
                .userId(user.getId())
                .parentReplyId(null)
                .content(request.getContent())
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        filmReviewReplyMapper.insert(reply);
        filmReviewMapper.incrementReplyCount(reviewId);

        return new DataResponse<>(SUCCESS, "Balasan berhasil ditambahkan", HttpStatus.CREATED.value(), mapToReplyResponse(reply, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<FilmReviewReplyResponse> updateReply(String slug, Long replyId, ReplyRequest request) {
        String username = requireUsername();
        User user = requireUser(username);

        FilmReviewReply reply = filmReviewReplyMapper.findById(replyId);
        if (reply == null) throw new DataNotFoundException();
        if (!reply.getUserId().equals(user.getId())) throw new UnauthorizedException();

        reply.setContent(request.getContent());
        reply.setUpdatedAt(LocalDateTime.now());
        filmReviewReplyMapper.update(reply);

        return new DataResponse<>(SUCCESS, "Balasan berhasil diperbarui", HttpStatus.OK.value(), mapToReplyResponse(reply, user.getId()));
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteReply(String slug, Long replyId) {
        String username = requireUsername();
        User user = requireUser(username);

        FilmReviewReply reply = filmReviewReplyMapper.findById(replyId);
        if (reply == null) throw new DataNotFoundException();
        if (!reply.getUserId().equals(user.getId())) throw new UnauthorizedException();

        filmReviewReplyMapper.softDelete(replyId);
        filmReviewMapper.decrementReplyCount(reply.getReviewId());

        return new DataResponse<>(SUCCESS, "Balasan dihapus", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> addOrUpdateFeedback(String slug, Long reviewId, FeedbackRequest request) {
        String username = requireUsername();
        User user = requireUser(username);
        requireFilm(slug);

        FilmReview review = filmReviewMapper.findById(reviewId);
        if (review == null) throw new DataNotFoundException();

        if (review.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Kamu tidak bisa memberi feedback pada ulasan sendiri");
        }

        FilmReviewFeedback existing = filmReviewFeedbackMapper.findByUserAndReview(user.getId(), reviewId);
        String message;

        if (existing != null) {
            existing.setIsHelpful(request.getIsHelpful());
            existing.setUpdatedAt(LocalDateTime.now());
            filmReviewFeedbackMapper.update(existing);
            message = "Feedback berhasil diperbarui";
        } else {
            FilmReviewFeedback feedback = FilmReviewFeedback.builder()
                    .reviewId(reviewId)
                    .userId(user.getId())
                    .isHelpful(request.getIsHelpful())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            filmReviewFeedbackMapper.insert(feedback);
            message = "Feedback berhasil ditambahkan";
        }

        filmReviewMapper.updateHelpfulCounts(reviewId);

        return new DataResponse<>(SUCCESS, message, HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteFeedback(String slug, Long reviewId) {
        String username = requireUsername();
        User user = requireUser(username);

        FilmReviewFeedback feedback = filmReviewFeedbackMapper.findByUserAndReview(user.getId(), reviewId);
        if (feedback == null) throw new DataNotFoundException();

        filmReviewFeedbackMapper.delete(feedback.getId());
        filmReviewMapper.updateHelpfulCounts(reviewId);

        return new DataResponse<>(SUCCESS, "Feedback dihapus", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> addToWatchlist(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        if (filmWatchlistMapper.findByUserAndFilm(user.getId(), film.getId()) != null) {
            return new DataResponse<>(SUCCESS, "Film sudah ada di watchlist", HttpStatus.OK.value(), null);
        }

        FilmWatchlist watchlist = FilmWatchlist.builder()
                .filmId(film.getId())
                .userId(user.getId())
                .addedAt(LocalDateTime.now())
                .build();

        filmWatchlistMapper.insert(watchlist);
        return new DataResponse<>(SUCCESS, "Film ditambahkan ke watchlist", HttpStatus.CREATED.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> removeFromWatchlist(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        FilmWatchlist watchlist = filmWatchlistMapper.findByUserAndFilm(user.getId(), film.getId());
        if (watchlist == null) throw new DataNotFoundException();

        filmWatchlistMapper.delete(watchlist.getId());
        return new DataResponse<>(SUCCESS, "Film dihapus dari watchlist", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<Boolean> isInWatchlist(String slug) {
        String username = requireUsername();
        User user = requireUser(username);
        Film film = requireFilm(slug);

        boolean inList = filmWatchlistMapper.findByUserAndFilm(user.getId(), film.getId()) != null;
        return new DataResponse<>(SUCCESS, "Status watchlist", HttpStatus.OK.value(), inList);
    }

    @Override
    public DatatableResponse<FilmWatchlistResponse> getMyWatchlist(int page, int limit) {
        String username = requireUsername();
        User user = requireUser(username);
        int offset = (page - 1) * limit;

        List<FilmWatchlistResponse> responses = filmWatchlistMapper.findByUserWithFilmInfo(user.getId(), offset, limit);
        int total = filmWatchlistMapper.countByUser(user.getId());

        PageDataResponse<FilmWatchlistResponse> pageData = new PageDataResponse<>(page, limit, total, responses);

        return new DatatableResponse<>(SUCCESS, "Watchlist berhasil diambil", HttpStatus.OK.value(), pageData);
    }

    @Override
    @Transactional
    public DataResponse<Void> updateWatchProgress(String slug, WatchProgressRequest request) {
        Film film = requireFilm(slug);
        Long userId = getCurrentUserId();

        boolean completed = request.getDurationSeconds() != null && request.getDurationSeconds() > 0 && request.getProgressSeconds() >= (int) (request.getDurationSeconds() * 0.9);

        WatchHistory existing = findExistingHistory(userId, request.getViewerHash(), film.getId());

        if (existing != null) {
            existing.setProgressSeconds(request.getProgressSeconds());
            existing.setDurationSeconds(request.getDurationSeconds());
            existing.setProviderType(request.getProviderType());
            existing.setVideoUrl(request.getVideoUrl());
            existing.setCompleted(completed);
            existing.setLastWatchedAt(LocalDateTime.now());
            watchHistoryMapper.update(existing);
        } else {
            WatchHistory history = WatchHistory.builder()
                    .filmId(film.getId())
                    .userId(userId)
                    .viewerHash(userId == null ? request.getViewerHash() : null)
                    .progressSeconds(request.getProgressSeconds())
                    .durationSeconds(request.getDurationSeconds())
                    .providerType(request.getProviderType())
                    .videoUrl(request.getVideoUrl())
                    .completed(completed)
                    .lastWatchedAt(LocalDateTime.now())
                    .build();
            watchHistoryMapper.insert(history);
        }

        return new DataResponse<>(SUCCESS, "Progress disimpan", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<WatchProgressResponse> getMyProgress(String slug) {
        Film film = requireFilm(slug);
        Long userId = getCurrentUserId();

        WatchHistory history = userId != null ? watchHistoryMapper.findByUserAndFilm(userId, film.getId()) : null;

        if (history == null) {
            return new DataResponse<>(SUCCESS, "Belum ada progress", HttpStatus.OK.value(), null);
        }

        WatchProgressResponse resp = new WatchProgressResponse();
        resp.setFilmId(film.getId());
        resp.setProgressSeconds(history.getProgressSeconds());
        resp.setDurationSeconds(history.getDurationSeconds());
        resp.setCompleted(history.getCompleted());
        resp.setProviderType(history.getProviderType());
        resp.setVideoUrl(history.getVideoUrl());

        double percentage = 0.0;
        if (history.getDurationSeconds() != null && history.getDurationSeconds() > 0) {
            percentage = Math.round(
                    (double) history.getProgressSeconds() / history.getDurationSeconds() * 1000.0
            ) / 10.0;
        }
        resp.setPercentage(percentage);

        return new DataResponse<>(SUCCESS, "Progress berhasil diambil", HttpStatus.OK.value(), resp);
    }

    private WatchHistory findExistingHistory(Long userId, String viewerHash, Long filmId) {
        if (userId != null) {
            return watchHistoryMapper.findByUserAndFilm(userId, filmId);
        }
        if (viewerHash != null && !viewerHash.isBlank()) {
            return watchHistoryMapper.findByHashAndFilm(viewerHash, filmId);
        }
        return null;
    }

    @Override
    public DataResponse<List<VideoSourceResponse>> getVideoSources(String slug) {
        Film film = requireFilm(slug);
        List<FilmVideoSource> sources = filmVideoSourceMapper.findActiveByFilmId(film.getId());
        List<VideoSourceResponse> responses = sources.stream()
                .map(this::mapToVideoSourceResponse)
                .toList();
        return new DataResponse<>(SUCCESS, "Video sources berhasil diambil", HttpStatus.OK.value(), responses);
    }

    @Override
    @Transactional
    public DataResponse<VideoSourceResponse> addVideoSource(String slug, AddVideoSourceRequest request) {
        requireAdmin();
        Film film = requireFilm(slug);

        FilmVideoSource source = videoProviderService.resolveToEntity(request.getUrl(), film.getId(), request.isTrailer());

        if (source == null) {
            throw new IllegalArgumentException("URL video tidak valid atau tidak didukung");
        }

        if (request.getPriority() != null) {
            source.setPriority(request.getPriority());
        }

        filmVideoSourceMapper.insert(source);
        log.info("[VideoSource] Added filmId={} provider={} trailer={}", film.getId(), source.getProviderType(), source.getIsTrailer());

        return new DataResponse<>(SUCCESS, "Video source berhasil ditambahkan", HttpStatus.CREATED.value(), mapToVideoSourceResponse(source));
    }

    @Override
    @Transactional
    public DataResponse<Void> removeVideoSource(String slug, Long sourceId) {
        requireAdmin();
        requireFilm(slug);

        FilmVideoSource source = filmVideoSourceMapper.findById(sourceId);
        if (source == null) throw new DataNotFoundException();

        filmVideoSourceMapper.delete(sourceId);
        return new DataResponse<>(SUCCESS, "Video source dihapus", HttpStatus.OK.value(), null);
    }

    private FilmRatingResponse mapToRatingResponse(FilmRating rating, User user) {
        FilmRatingResponse resp = new FilmRatingResponse();
        resp.setId(rating.getId());
        resp.setFilmId(rating.getFilmId());
        resp.setUserId(rating.getUserId());
        resp.setUsername(user.getUsername());
        resp.setUserPhotoUrl(user.getProfilePictureUrl());
        resp.setRating(rating.getRating());
        resp.setCreatedAt(rating.getCreatedAt());
        resp.setUpdatedAt(rating.getUpdatedAt());
        return resp;
    }

    private FilmReviewResponse mapToReviewResponse(FilmReview review, Long currentUserId) {
        User reviewUser = userMapper.findUserById(review.getUserId());

        FilmReviewResponse resp = new FilmReviewResponse();
        resp.setId(review.getId());
        resp.setFilmId(review.getFilmId());
        resp.setUserId(review.getUserId());
        resp.setUsername(reviewUser != null ? reviewUser.getUsername() : "Unknown");
        resp.setUserPhotoUrl(reviewUser != null ? reviewUser.getProfilePictureUrl() : null);
        resp.setTitle(review.getTitle());
        resp.setContent(review.getContent());
        resp.setHelpfulCount(review.getHelpfulCount());
        resp.setNotHelpfulCount(review.getNotHelpfulCount());
        resp.setReplyCount(review.getReplyCount());
        resp.setCreatedAt(review.getCreatedAt());
        resp.setUpdatedAt(review.getUpdatedAt());
        resp.setIsOwner(currentUserId != null && currentUserId.equals(review.getUserId()));

        if (currentUserId != null) {
            FilmReviewFeedback feedback = filmReviewFeedbackMapper.findByUserAndReview(currentUserId, review.getId());
            resp.setCurrentUserFeedback(feedback != null ? feedback.getIsHelpful() : null);
        } else {
            resp.setCurrentUserFeedback(null);
        }

        List<FilmReviewReply> replies = filmReviewReplyMapper.findByReviewId(review.getId());
        resp.setReplies(replies.stream()
                .map(r -> mapToReplyResponse(r, currentUserId))
                .toList());

        return resp;
    }

    private FilmReviewReplyResponse mapToReplyResponse(FilmReviewReply reply, Long currentUserId) {
        User replyUser = userMapper.findUserById(reply.getUserId());

        FilmReviewReplyResponse resp = new FilmReviewReplyResponse();
        resp.setId(reply.getId());
        resp.setUserId(reply.getUserId());
        resp.setUsername(replyUser != null ? replyUser.getUsername() : "Unknown");
        resp.setUserPhotoUrl(replyUser != null ? replyUser.getProfilePictureUrl() : null);
        resp.setReviewId(reply.getReviewId());
        resp.setParentReplyId(reply.getParentReplyId());
        resp.setContent(reply.getContent());
        resp.setCreatedAt(reply.getCreatedAt());
        resp.setUpdatedAt(reply.getUpdatedAt());
        resp.setIsOwner(currentUserId != null && currentUserId.equals(reply.getUserId()));
        return resp;
    }

    private VideoSourceResponse mapToVideoSourceResponse(FilmVideoSource source) {
        VideoSourceResponse resp = new VideoSourceResponse();
        resp.setId(source.getId());
        resp.setRawUrl(source.getRawUrl());
        resp.setProviderType(source.getProviderType());
        resp.setEmbedUrl(source.getEmbedUrl());
        resp.setDirectUrl(source.getDirectUrl());
        resp.setThumbnailUrl(source.getThumbnailUrl());
        resp.setTitle(source.getTitle());
        resp.setDurationSeconds(source.getDurationSeconds());
        resp.setIsTrailer(source.getIsTrailer());
        resp.setPriority(source.getPriority());
        return resp;
    }

    private int nullToZero(Integer val) {
        return val == null ? 0 : val;
    }
}