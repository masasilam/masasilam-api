package com.masasilam.app.mapper.social;

import com.masasilam.app.model.entity.social.*;
import com.masasilam.app.model.dto.response.social.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SocialActivityMapper {
    void insertActivity(SocialActivity activity);
    void updateActivity(SocialActivity activity);
    void softDeleteActivity(Long id);

    // Feed queries
    List<ActivityFeedItemResponse> getFollowingFeed(@Param("userId") Long userId,
                                                    @Param("offset") int offset, @Param("limit") int limit);
    List<ActivityFeedItemResponse> getPublicFeed(@Param("offset") int offset, @Param("limit") int limit);
    List<ActivityFeedItemResponse> getUserActivities(@Param("profileUserId") Long profileUserId,
                                                     @Param("currentUserId") Long currentUserId,
                                                     @Param("offset") int offset, @Param("limit") int limit);
    int countFollowingFeed(Long userId);

    // Likes
    void insertLike(ActivityLike like);
    void deleteLike(@Param("activityId") Long activityId, @Param("userId") Long userId);
    ActivityLike findLike(@Param("activityId") Long activityId, @Param("userId") Long userId);
    void incrementLikeCount(Long activityId);
    void decrementLikeCount(Long activityId);

    // Comments
    void insertComment(ActivityComment comment);
    void updateComment(ActivityComment comment);
    void softDeleteComment(Long id);
    List<ActivityCommentResponse> findCommentsByActivity(@Param("activityId") Long activityId,
                                                         @Param("currentUserId") Long currentUserId);
    ActivityComment findCommentById(Long id);
    void incrementCommentCount(Long activityId);
    void decrementCommentCount(Long activityId);

    // Stats
    int countUserActivities(Long userId);
    int countUserLikesReceived(Long userId);
    int countUserCommentsReceived(Long userId);
}