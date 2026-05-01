package com.naskah.demo.service.social;

import com.naskah.demo.model.dto.request.social.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.dto.response.social.*;

public interface ActivityFeedService {
    DataResponse<FeedPageResponse> getFollowingFeed(int page, int limit);
    DataResponse<FeedPageResponse> getPublicFeed(int page, int limit);
    DataResponse<FeedPageResponse> getUserFeed(Long userId, int page, int limit);
    DataResponse<Void> likeActivity(Long activityId);
    DataResponse<Void> unlikeActivity(Long activityId);
    DataResponse<ActivityCommentResponse> commentOnActivity(Long activityId, ActivityCommentRequest request);
    DataResponse<ActivityCommentResponse> updateActivityComment(Long commentId, ActivityCommentRequest request);
    DataResponse<Void> deleteActivityComment(Long commentId);
    DataResponse<FeedPageResponse> getActivityComments(Long activityId, int page, int limit);
    // Internal use
    void publishActivity(Long userId, String activityType, String entityType,
                         Long entityId, String entitySlug, String entityTitle,
                         String entityCover, String metadataJson, String visibility);
}