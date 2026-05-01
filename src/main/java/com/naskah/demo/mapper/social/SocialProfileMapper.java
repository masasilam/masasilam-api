package com.naskah.demo.mapper.social;

import com.naskah.demo.model.entity.social.*;
import com.naskah.demo.model.dto.response.social.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SocialProfileMapper {
    UserSocialProfile findByUserId(Long userId);
    void insert(UserSocialProfile profile);
    void update(UserSocialProfile profile);
    UserPublicProfileResponse getPublicProfile(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId);
    List<FollowerItemResponse> findFollowers(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId,
                                             @Param("offset") int offset, @Param("limit") int limit);
    List<FollowerItemResponse> findFollowing(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId,
                                             @Param("offset") int offset, @Param("limit") int limit);
    int countFollowers(Long userId);
    int countFollowing(Long userId);
}