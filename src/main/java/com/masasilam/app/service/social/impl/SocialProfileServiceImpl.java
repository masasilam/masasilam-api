package com.masasilam.app.service.social.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.mapper.social.*;
import com.masasilam.app.model.dto.request.social.*;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.social.*;
import com.masasilam.app.service.social.*;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialProfileServiceImpl implements SocialProfileService {
    private final SocialProfileMapper profileMapper;
    private final UserFollowMapper followMapper;
    private final SocialActivityMapper activityMapper;
    private final SocialAnnotationMapper annotationMapper;
    private final ReadingListMapper listMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final HeaderHolder headerHolder;
    private final ObjectMapper objectMapper;

    private static final String SUCCESS = "Success";
    private static final String PUBLIC = "public";
    private static final String DEFAULT = "default";

    private User requireAuth() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private Long currentUserIdOrNull() {
        try {
            String username = headerHolder.getUsername();
            if (username == null || username.isBlank()) return null;
            User u = userMapper.findUserByUsername(username);
            return u != null ? u.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public DataResponse<UserPublicProfileResponse> getPublicProfile(Long userId) {
        Long currentUserId = currentUserIdOrNull();
        UserPublicProfileResponse profile = profileMapper.getPublicProfile(userId, currentUserId);
        if (profile == null) throw new DataNotFoundException();

        List<ActivityFeedItemResponse> recent = activityMapper.getUserActivities(userId, currentUserId, 0, 5);
        profile.setRecentActivities(recent);

        List<ReadingListResponse> lists = listMapper.findByUser(userId, currentUserId, 0, 6);
        List<ReadingListSummaryResponse> summaries = lists.stream()
                .map(this::toSummary).toList();
        profile.setPublicLists(summaries);

        List<SocialAnnotationResponse> annotations = annotationMapper.findByUser(userId, currentUserId, 0, 6);
        profile.setPublicAnnotations(annotations);
        profile.setFollowerCount(profileMapper.countFollowers(userId));
        profile.setFollowingCount(profileMapper.countFollowing(userId));
        profile.setTotalActivities(activityMapper.countUserActivities(userId));
        profile.setTotalReadingLists(listMapper.countByUser(userId));

        return new DataResponse<>(SUCCESS, "Profile retrieved", HttpStatus.OK.value(), profile);
    }

    @Override
    public DataResponse<UserPublicProfileResponse> getPublicProfileByUsername(String username) {
        User user = userMapper.findUserByUsername(username);
        if (user == null || !user.getIsActive()) throw new DataNotFoundException();
        return getPublicProfile(user.getId());
    }

    @Override
    @Transactional
    public DataResponse<UserPublicProfileResponse> updateMyProfile(UpdateSocialProfileRequest request) {
        User me = requireAuth();

        UserSocialProfile profile = profileMapper.findByUserId(me.getId());
        if (profile == null) {
            profile = new UserSocialProfile();
            profile.setUserId(me.getId());
        }

        if (request.getDisplayName() != null) profile.setDisplayName(request.getDisplayName());
        if (request.getTagline() != null) profile.setTagline(request.getTagline());
        if (request.getLocation() != null) profile.setLocation(request.getLocation());
        if (request.getWebsiteUrl() != null) profile.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getReadingVisibility() != null) profile.setReadingVisibility(request.getReadingVisibility());
        if (request.getAnnotationVisibility() != null)
            profile.setAnnotationVisibility(request.getAnnotationVisibility());
        if (request.getProfileTheme() != null) profile.setProfileTheme(request.getProfileTheme());

        if (request.getSocialLinks() != null) {
            try {
                profile.setSocialLinks(objectMapper.writeValueAsString(request.getSocialLinks()));
            } catch (Exception e) {
                profile.setSocialLinks("{}");
            }
        }

        if (profile.getId() == null) profileMapper.insert(profile);
        else profileMapper.update(profile);

        return getPublicProfile(me.getId());
    }

    @Override
    public DataResponse<SocialStatsResponse> getMyStats() {
        User me = requireAuth();
        SocialStatsResponse stats = new SocialStatsResponse();
        stats.setTotalActivities(activityMapper.countUserActivities(me.getId()));
        stats.setTotalLikesReceived(activityMapper.countUserLikesReceived(me.getId()));
        stats.setTotalCommentsReceived(activityMapper.countUserCommentsReceived(me.getId()));
        stats.setTotalFollowers(profileMapper.countFollowers(me.getId()));
        stats.setTotalFollowing(profileMapper.countFollowing(me.getId()));
        stats.setMutualFollows(followMapper.countMutualFollows(me.getId(), me.getId()));
        stats.setTotalPublicAnnotations(annotationMapper.countByUser(me.getId()));
        stats.setTotalReadingLists(listMapper.countByUser(me.getId()));
        return new DataResponse<>(SUCCESS, "Stats retrieved", HttpStatus.OK.value(), stats);
    }

    @Override
    public DataResponse<FollowStatsResponse> getFollowers(Long userId, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        FollowStatsResponse response = new FollowStatsResponse();
        response.setUserId(userId);
        response.setTotalFollowers(profileMapper.countFollowers(userId));
        response.setTotalFollowing(profileMapper.countFollowing(userId));
        response.setFollowers(profileMapper.findFollowers(userId, currentUserId, offset, limit));
        return new DataResponse<>(SUCCESS, "Followers retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    public DataResponse<FollowStatsResponse> getFollowing(Long userId, int page, int limit) {
        Long currentUserId = currentUserIdOrNull();
        int offset = (page - 1) * limit;
        FollowStatsResponse response = new FollowStatsResponse();
        response.setUserId(userId);
        response.setTotalFollowers(profileMapper.countFollowers(userId));
        response.setTotalFollowing(profileMapper.countFollowing(userId));
        response.setFollowing(profileMapper.findFollowing(userId, currentUserId, offset, limit));
        return new DataResponse<>(SUCCESS, "Following retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    @Transactional
    public DataResponse<Void> followUser(Long targetUserId) {
        User me = requireAuth();
        if (me.getId().equals(targetUserId))
            throw new BadRequestException("Cannot follow yourself");

        User target = userMapper.findUserById(targetUserId);
        if (target == null || !target.getIsActive()) throw new DataNotFoundException();

        boolean already = followMapper.isFollowing(me.getId(), targetUserId);
        if (already) throw new BadRequestException("Already following this user");

        ensureProfileExists(me.getId());
        ensureProfileExists(targetUserId);

        UserFollow follow = new UserFollow();
        follow.setFollowerId(me.getId());
        follow.setFollowingId(targetUserId);
        followMapper.insert(follow);

        notificationService.sendNotification(
                targetUserId, me.getId(), "new_follower",
                "USER", me.getId(),
                me.getUsername() + " mulai mengikuti kamu",
                "{}"
        );

        publishFollowActivity(me, target);

        log.info("User {} followed {}", me.getId(), targetUserId);
        return new DataResponse<>(SUCCESS, "Now following " + target.getUsername(),
                HttpStatus.OK.value(), null);
    }

    @Override
    @Transactional
    public DataResponse<Void> unfollowUser(Long targetUserId) {
        User me = requireAuth();
        boolean exists = followMapper.isFollowing(me.getId(), targetUserId);
        if (!exists) throw new DataNotFoundException();
        followMapper.delete(me.getId(), targetUserId);
        log.info("User {} unfollowed {}", me.getId(), targetUserId);
        return new DataResponse<>(SUCCESS, "Unfollowed successfully", HttpStatus.OK.value(), null);
    }

    @Override
    public DataResponse<Boolean> checkIsFollowing(Long targetUserId) {
        User me = requireAuth();
        boolean following = followMapper.isFollowing(me.getId(), targetUserId);
        return new DataResponse<>(SUCCESS, "Check complete", HttpStatus.OK.value(), following);
    }

    private void ensureProfileExists(Long userId) {
        if (profileMapper.findByUserId(userId) == null) {
            UserSocialProfile p = new UserSocialProfile();
            p.setUserId(userId);
            p.setReadingVisibility(PUBLIC);
            p.setAnnotationVisibility(PUBLIC);
            p.setProfileTheme(DEFAULT);
            profileMapper.insert(p);
        }
    }

    private void publishFollowActivity(User follower, User followed) {
        try {
            SocialActivity activity = new SocialActivity();
            activity.setUserId(follower.getId());
            activity.setActivityType("followed_user");
            activity.setEntityType("USER");
            activity.setEntityId(followed.getId());
            activity.setEntitySlug(followed.getUsername());
            activity.setEntityTitle(followed.getUsername());
            activity.setEntityCover(followed.getProfilePictureUrl());
            activity.setMetadata("{}");
            activity.setVisibility(PUBLIC);
            activityMapper.insertActivity(activity);
        } catch (Exception e) {
            log.warn("Failed to publish follow activity: {}", e.getMessage());
        }
    }

    private ReadingListSummaryResponse toSummary(ReadingListResponse r) {
        ReadingListSummaryResponse s = new ReadingListSummaryResponse();
        s.setId(r.getId());
        s.setTitle(r.getTitle());
        s.setSlug(r.getSlug());
        s.setCoverImageUrl(r.getCoverImageUrl());
        s.setItemCount(r.getItemCount());
        s.setLikeCount(r.getLikeCount());
        s.setTags(r.getTags());
        return s;
    }
}