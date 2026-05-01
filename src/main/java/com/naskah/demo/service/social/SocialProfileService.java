package com.naskah.demo.service.social;

import com.naskah.demo.model.dto.request.social.*;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.dto.response.social.*;

public interface SocialProfileService {
    DataResponse<UserPublicProfileResponse> getPublicProfile(Long userId);
    DataResponse<UserPublicProfileResponse> getPublicProfileByUsername(String username);
    DataResponse<UserPublicProfileResponse> updateMyProfile(UpdateSocialProfileRequest request);
    DataResponse<SocialStatsResponse> getMyStats();
    DataResponse<FollowStatsResponse> getFollowers(Long userId, int page, int limit);
    DataResponse<FollowStatsResponse> getFollowing(Long userId, int page, int limit);
    DataResponse<Void> followUser(Long targetUserId);
    DataResponse<Void> unfollowUser(Long targetUserId);
    DataResponse<Boolean> checkIsFollowing(Long targetUserId);
}