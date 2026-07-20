package com.masasilam.app.service.social.impl;

import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.UserMapper;
import com.masasilam.app.mapper.social.SocialActivityMapper;
import com.masasilam.app.model.dto.request.social.ActivityCommentRequest;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.social.*;
import com.masasilam.app.service.social.ActivityFeedService;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityFeedServiceImpl implements ActivityFeedService {
    private final SocialActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";

    private User requireAuth() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private Long currentUserIdOrNull() {
        try {
            String u = headerHolder.getUsername();
            if (u == null || u.isBlank()) return null;
            User user = userMapper.findUserByUsername(u);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public DataResponse<FeedPageResponse> getFollowingFeed(int page, int limit) {
        User me = requireAuth();
        int offset = (page - 1) * limit;
        List<ActivityFeedItemResponse> items = activityMapper.getFollowingFeed(me.getId(), offset, limit);
        items.forEach(this::enrichItem);
        int total = activityMapper.countFollowingFeed(me.getId());

        FeedPageResponse feed = buildFeedPage(items, total, page, limit);
        return new DataResponse<>(SUCCESS, "Feed retrieved", HttpStatus.OK.value(), feed);
    }

    @Override
    public DataResponse<FeedPageResponse> getPublicFeed(int page, int limit) {
        int offset = (page - 1) * limit;
        List<ActivityFeedItemResponse> items = activityMapper.getPublicFeed(offset, limit);
        items.forEach(this::enrichItem);
        FeedPageResponse feed = buildFeedPage(items, items.size(), page, limit);
        return new DataResponse<>(SUCCESS, "Public feed retrieved", HttpStatus.OK.value(), feed);
    }

    @Override
    public DataResponse<FeedPageResponse> getUserFeed(Long userId, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        List<ActivityFeedItemResponse> items = activityMapper.getUserActivities(userId, currentUserId, offset, limit);
        items.forEach(this::enrichItem);
        int total = activityMapper.countUserActivities(userId);
        FeedPageResponse feed = buildFeedPage(items, total, page, limit);
        return new DataResponse<>(SUCCESS, "User feed retrieved", HttpStatus.OK.value(), feed);
    }

    @Override
    @Transactional
    public DataResponse<Void> likeActivity(Long activityId) {
        User me = requireAuth();
        ActivityLike existing = activityMapper.findLike(activityId, me.getId());
        if (existing != null) throw new BadRequestException("Already liked");

        ActivityLike like = new ActivityLike();
        like.setActivityId(activityId);
        like.setUserId(me.getId());
        activityMapper.insertLike(like);
        activityMapper.incrementLikeCount(activityId);
        return new DataResponse<>(SUCCESS, "Liked", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> unlikeActivity(Long activityId) {
        User me = requireAuth();
        ActivityLike existing = activityMapper.findLike(activityId, me.getId());
        if (existing == null) throw new DataNotFoundException();
        activityMapper.deleteLike(activityId, me.getId());
        activityMapper.decrementLikeCount(activityId);
        return new DataResponse<>(SUCCESS, "Unliked", HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<ActivityCommentResponse> commentOnActivity(Long activityId, ActivityCommentRequest request) {
        User me = requireAuth();

        ActivityComment comment = new ActivityComment();
        comment.setActivityId(activityId);
        comment.setUserId(me.getId());
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        activityMapper.insertComment(comment);
        activityMapper.incrementCommentCount(activityId);

        ActivityCommentResponse response = buildCommentResponse(comment, me);
        return new DataResponse<>(SUCCESS, "Comment added", HttpStatus.CREATED.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<ActivityCommentResponse> updateActivityComment(Long commentId, ActivityCommentRequest request) {
        User me = requireAuth();
        ActivityComment comment = activityMapper.findCommentById(commentId);
        if (comment == null || comment.getIsDeleted()) throw new DataNotFoundException();
        if (!comment.getUserId().equals(me.getId())) throw new UnauthorizedException();

        comment.setContent(request.getContent());
        activityMapper.updateComment(comment);

        ActivityCommentResponse response = buildCommentResponse(comment, me);
        return new DataResponse<>(SUCCESS, "Comment updated", HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<Void> deleteActivityComment(Long commentId) {
        User me = requireAuth();
        ActivityComment comment = activityMapper.findCommentById(commentId);
        if (comment == null || comment.getIsDeleted()) throw new DataNotFoundException();
        if (!comment.getUserId().equals(me.getId())) throw new UnauthorizedException();

        activityMapper.softDeleteComment(commentId);
        activityMapper.decrementCommentCount(comment.getActivityId());
        return new DataResponse<>(SUCCESS, "Comment deleted", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<FeedPageResponse> getActivityComments(Long activityId, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        List<ActivityCommentResponse> comments = activityMapper.findCommentsByActivity(activityId, currentUserId);
        FeedPageResponse feed = new FeedPageResponse();
        feed.setTotal(comments.size());
        feed.setPage(page);
        feed.setLimit(limit);
        feed.setHasMore(false);
        return new DataResponse<>(SUCCESS, "Comments retrieved", HttpStatus.OK.value(), feed);
    }

    @Override
    public void publishActivity(Long userId, String activityType, String entityType,
                                Long entityId, String entitySlug, String entityTitle,
                                String entityCover, String metadataJson, String visibility) {
        try {
            SocialActivity activity = new SocialActivity();
            activity.setUserId(userId);
            activity.setActivityType(activityType);
            activity.setEntityType(entityType);
            activity.setEntityId(entityId);
            activity.setEntitySlug(entitySlug);
            activity.setEntityTitle(entityTitle);
            activity.setEntityCover(entityCover);
            activity.setMetadata(metadataJson != null ? metadataJson : "{}");
            activity.setVisibility(visibility != null ? visibility : "public");
            activityMapper.insertActivity(activity);
        } catch (Exception e) {
            log.warn("publishActivity failed for user={} type={}: {}", userId, activityType, e.getMessage());
        }
    }

    private void enrichItem(ActivityFeedItemResponse item) {
        item.setTimeAgo(timeAgo(item.getCreatedAt().toLocalDateTime()));
    }

    private FeedPageResponse buildFeedPage(List<ActivityFeedItemResponse> items, int total, int page, int limit) {
        FeedPageResponse feed = new FeedPageResponse();
        feed.setItems(items);
        feed.setTotal(total);
        feed.setPage(page);
        feed.setLimit(limit);
        feed.setHasMore((long) page * limit < total);
        return feed;
    }

    private ActivityCommentResponse buildCommentResponse(ActivityComment c, User user) {
        ActivityCommentResponse r = new ActivityCommentResponse();
        r.setId(c.getId());
        r.setActivityId(c.getActivityId());
        r.setUserId(c.getUserId());
        r.setUsername(user.getUsername());
        r.setUserPhoto(user.getProfilePictureUrl());
        r.setParentId(c.getParentId());
        r.setContent(c.getContent());
        r.setIsOwner(true);
        r.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.now());
        r.setUpdatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt() : LocalDateTime.now());
        return r;
    }

    private String timeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        Duration d = Duration.between(dateTime, LocalDateTime.now());
        if (d.toMinutes() < 1) return "baru saja";
        if (d.toMinutes() < 60) return d.toMinutes() + " menit lalu";
        if (d.toHours() < 24) return d.toHours() + " jam lalu";
        if (d.toDays() < 7) return d.toDays() + " hari lalu";
        if (d.toDays() < 30) return (d.toDays() / 7) + " minggu lalu";
        if (d.toDays() < 365) return (d.toDays() / 30) + " bulan lalu";
        return (d.toDays() / 365) + " tahun lalu";
    }
}