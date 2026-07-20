package com.naskah.app.controller.social;

import com.naskah.app.model.dto.request.social.ActivityCommentRequest;
import com.naskah.app.model.dto.response.*;
import com.naskah.app.model.dto.response.social.*;
import com.naskah.app.service.social.ActivityFeedService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/social/feed")
@RequiredArgsConstructor
public class ActivityFeedController {
    private final ActivityFeedService feedService;

    @GetMapping("/following")
    public ResponseEntity<DataResponse<FeedPageResponse>> getFollowingFeed(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                           @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(feedService.getFollowingFeed(page, limit));
    }

    @GetMapping("/public")
    public ResponseEntity<DataResponse<FeedPageResponse>> getPublicFeed(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                                        @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(feedService.getPublicFeed(page, limit));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<DataResponse<FeedPageResponse>> getUserFeed(@PathVariable Long userId,
                                                                      @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                      @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(feedService.getUserFeed(userId, page, limit));
    }

    @PostMapping("/activities/{activityId}/like")
    public ResponseEntity<DataResponse<Void>> likeActivity(@PathVariable Long activityId) {
        return ResponseEntity.ok(feedService.likeActivity(activityId));
    }

    @DeleteMapping("/activities/{activityId}/like")
    public ResponseEntity<DataResponse<Void>> unlikeActivity(@PathVariable Long activityId) {
        return ResponseEntity.ok(feedService.unlikeActivity(activityId));
    }

    @GetMapping("/activities/{activityId}/comments")
    public ResponseEntity<DataResponse<FeedPageResponse>> getActivityComments(@PathVariable Long activityId,
                                                                              @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                              @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(feedService.getActivityComments(activityId, page, limit));
    }

    @PostMapping("/activities/{activityId}/comments")
    public ResponseEntity<DataResponse<ActivityCommentResponse>> commentOnActivity(@PathVariable Long activityId,
                                                                                   @Valid @RequestBody ActivityCommentRequest request) {
        return ResponseEntity.ok(feedService.commentOnActivity(activityId, request));
    }

    @PutMapping("/activities/comments/{commentId}")
    public ResponseEntity<DataResponse<ActivityCommentResponse>> updateActivityComment(@PathVariable Long commentId,
                                                                                       @Valid @RequestBody ActivityCommentRequest request) {
        return ResponseEntity.ok(feedService.updateActivityComment(commentId, request));
    }

    @DeleteMapping("/activities/comments/{commentId}")
    public ResponseEntity<DataResponse<Void>> deleteActivityComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(feedService.deleteActivityComment(commentId));
    }
}