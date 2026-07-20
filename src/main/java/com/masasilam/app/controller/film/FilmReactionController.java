package com.masasilam.app.controller.film;

import com.masasilam.app.model.dto.request.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.service.film.FilmReactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/films/{slug}")
@RequiredArgsConstructor
public class FilmReactionController {
    private final FilmReactionService reactionService;

    @PostMapping(value = "/rating", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataResponse<FilmRatingResponse>> addOrUpdateRating(@PathVariable String slug, @Valid @RequestBody RatingRequest request) {
        DataResponse<FilmRatingResponse> resp = reactionService.addOrUpdateRating(slug, request);
        return ResponseEntity.status(resp.getCode()).body(resp);
    }

    @GetMapping("/rating")
    public ResponseEntity<DataResponse<FilmRatingStatsResponse>> getRatingStats(@PathVariable String slug) {
        return ResponseEntity.ok(reactionService.getRatingStats(slug));
    }

    @GetMapping("/rating/me")
    public ResponseEntity<DataResponse<FilmRatingResponse>> getMyRating(@PathVariable String slug) {
        return ResponseEntity.ok(reactionService.getMyRating(slug));
    }

    @DeleteMapping("/rating")
    public ResponseEntity<DataResponse<Void>> deleteRating(@PathVariable String slug) {
        return ResponseEntity.ok(reactionService.deleteRating(slug));
    }

    @GetMapping("/reviews")
    public ResponseEntity<DatatableResponse<FilmReviewResponse>> getReviews(@PathVariable String slug,
                                                                            @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                            @RequestParam(defaultValue = "10") @Min(1) int limit,
                                                                            @RequestParam(defaultValue = "helpful") String sortBy) {
        return ResponseEntity.ok(reactionService.getReviews(slug, page, limit, sortBy));
    }

    @GetMapping("/reviews/me")
    public ResponseEntity<DataResponse<FilmReviewResponse>> getMyReview(@PathVariable String slug) {
        return ResponseEntity.ok(reactionService.getMyReview(slug));
    }

    @PostMapping(value = "/reviews", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataResponse<FilmReviewResponse>> createReview(@PathVariable String slug, @Valid @RequestBody FilmReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reactionService.createReview(slug, request));
    }

    @PutMapping(value = "/reviews", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataResponse<FilmReviewResponse>> updateReview(@PathVariable String slug, @Valid @RequestBody FilmReviewRequest request) {
        return ResponseEntity.ok(reactionService.updateReview(slug, request));
    }

    @DeleteMapping("/reviews")
    public ResponseEntity<DataResponse<Void>> deleteReview(@PathVariable String slug) {
        return ResponseEntity.ok(reactionService.deleteReview(slug));
    }

    @PostMapping(value = "/reviews/{reviewId}/replies", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataResponse<FilmReviewReplyResponse>> addReply(@PathVariable String slug,
                                                                          @PathVariable Long reviewId,
                                                                          @Valid @RequestBody ReplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reactionService.addReply(slug, reviewId, request));
    }

    @PutMapping(value = "/reviews/replies/{replyId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataResponse<FilmReviewReplyResponse>> updateReply(@PathVariable String slug,
                                                                             @PathVariable Long replyId,
                                                                             @Valid @RequestBody ReplyRequest request) {
        return ResponseEntity.ok(reactionService.updateReply(slug, replyId, request));
    }

    @DeleteMapping("/reviews/replies/{replyId}")
    public ResponseEntity<DataResponse<Void>> deleteReply(@PathVariable String slug, @PathVariable Long replyId) {
        return ResponseEntity.ok(reactionService.deleteReply(slug, replyId));
    }

    @PostMapping(value = "/reviews/{reviewId}/feedback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataResponse<Void>> addOrUpdateFeedback(@PathVariable String slug, @PathVariable Long reviewId, @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(reactionService.addOrUpdateFeedback(slug, reviewId, request));
    }

    @DeleteMapping("/reviews/{reviewId}/feedback")
    public ResponseEntity<DataResponse<Void>> deleteFeedback(@PathVariable String slug, @PathVariable Long reviewId) {
        return ResponseEntity.ok(reactionService.deleteFeedback(slug, reviewId));
    }

    @PostMapping("/watchlist")
    public ResponseEntity<DataResponse<Void>> addToWatchlist(@PathVariable String slug) {
        DataResponse<Void> resp = reactionService.addToWatchlist(slug);
        return ResponseEntity.status(resp.getCode()).body(resp);
    }

    @DeleteMapping("/watchlist")
    public ResponseEntity<DataResponse<Void>> removeFromWatchlist(@PathVariable String slug) {
        return ResponseEntity.ok(reactionService.removeFromWatchlist(slug));
    }

    @GetMapping("/watchlist/me")
    public ResponseEntity<DataResponse<Boolean>> isInWatchlist(@PathVariable String slug) {
        return ResponseEntity.ok(reactionService.isInWatchlist(slug));
    }

    @PostMapping(value = "/watch-progress", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataResponse<Void>> updateWatchProgress(@PathVariable String slug, @Valid @RequestBody WatchProgressRequest request) {
        return ResponseEntity.ok(reactionService.updateWatchProgress(slug, request));
    }

    @GetMapping("/watch-progress/me")
    public ResponseEntity<DataResponse<WatchProgressResponse>> getMyProgress(@PathVariable String slug) {
        return ResponseEntity.ok(reactionService.getMyProgress(slug));
    }

    @GetMapping("/video-sources")
    public ResponseEntity<DataResponse<List<VideoSourceResponse>>> getVideoSources(@PathVariable String slug) {
        return ResponseEntity.ok(reactionService.getVideoSources(slug));
    }

    @PostMapping(value = "/video-sources", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataResponse<VideoSourceResponse>> addVideoSource(@PathVariable String slug, @Valid @RequestBody AddVideoSourceRequest request) {
        DataResponse<VideoSourceResponse> resp = reactionService.addVideoSource(slug, request);
        return ResponseEntity.status(resp.getCode()).body(resp);
    }

    @DeleteMapping("/video-sources/{sourceId}")
    public ResponseEntity<DataResponse<Void>> removeVideoSource(@PathVariable String slug, @PathVariable Long sourceId) {
        return ResponseEntity.ok(reactionService.removeVideoSource(slug, sourceId));
    }
}